package controllers.paye

import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.common.microservice.paye.domain.Benefit
import org.joda.time.{Interval, DateTime, LocalDate}
import uk.gov.hmrc.common.microservice.paye.domain.BenefitTypes._
import play.api.data.Form
import play.api.data.Forms._
import controllers.paye.validation.RemoveBenefitValidator._
import play.api.mvc.Request
import uk.gov.hmrc.utils.TaxYearResolver
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import models.paye.{RemoveFuelBenefitFormData, CarFuelBenefitDates, RemoveCarBenefitFormData}
import uk.gov.hmrc.common.microservice.paye.domain.TaxYearData
import scala.Some


object RemovalUtils {

  val keystoreKey = "remove_benefit"
  private final val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  private final val dateRegex = """(\d\d\d\d-\d\d-\d\d)""".r

  def updateRemoveCarBenefitForm(values: Option[RemoveCarBenefitFormDataValues],
                        benefitStartDate: LocalDate,
                        carBenefitWithUnremovedFuelBenefit: Boolean,
                        dates: Option[CarFuelBenefitDates],
                        now: DateTime, taxYearInterval:Interval) = Form[RemoveCarBenefitFormData](
    mapping(
      "withdrawDate" -> localDateMapping(Some(benefitStartDate), now.toLocalDate, taxYearInterval),
      "carUnavailable" -> validateMandatoryBoolean,
      "numberOfDaysUnavailable" -> validateNumberOfDaysUnavailable(values, benefitStartDate, taxYearInterval),
      "employeeContributes" -> validateMandatoryBoolean,
      "employeeContribution" -> validateEmployeeContribution(values),
      "fuelRadio" -> validateFuelDateChoice(carBenefitWithUnremovedFuelBenefit),
      "fuelWithdrawDate" -> validateFuelDate(dates, Some(benefitStartDate), taxYearInterval)
    )(RemoveCarBenefitFormData.apply)(RemoveCarBenefitFormData.unapply)
  )

  def updateRemoveFuelBenefitForm(benefitStartDate: LocalDate, now: DateTime, taxYearInterval:Interval) = Form[RemoveFuelBenefitFormData](
    mapping(
      "withdrawDate" -> localDateMapping(Some(benefitStartDate), now.toLocalDate, taxYearInterval)
    )(RemoveFuelBenefitFormData.apply)(RemoveFuelBenefitFormData.unapply)
  )

  def getStartDate(benefit: Benefit): LocalDate = {
    val pathIncludingStartDate = benefit.calculations.get(PayeConnector.calculationWithdrawKey).getOrElse("")

    val benefitStartDate = dateRegex.findFirstIn(pathIncludingStartDate) map {
      dateFormat.parseLocalDate
    }

    benefitStartDate match {
      case Some(dateOfBenefitStart) if dateOfBenefitStart.isAfter(TaxYearResolver.startOfCurrentTaxYear) => dateOfBenefitStart
      case _ => TaxYearResolver.startOfCurrentTaxYear
    }
  }

  def getDatesFromDefaults(defaults: Option[RemoveCarBenefitFormData]): Option[CarFuelBenefitDates] = {
    defaults.map {
      formData =>
        CarFuelBenefitDates(Some(formData.withdrawDate), formData.fuelDateChoice)
    }.orElse {
      Some(CarFuelBenefitDates(None, None))
    }
  }

  def hasUnremovedFuelBenefit(payeRootData: TaxYearData, employmentNumber: Int): Boolean = {
    payeRootData.findActiveBenefit(employmentNumber, FUEL).isDefined
  }

  def datesForm() = Form[CarFuelBenefitDates](
    mapping(
      "withdrawDate" -> dateTuple(validate = false),
      "fuelRadio" -> optional(text)
    )(CarFuelBenefitDates.apply)(CarFuelBenefitDates.unapply)
  )

  def getCarFuelBenefitDates(request: Request[_]): Option[CarFuelBenefitDates] = {
    datesForm().bindFromRequest()(request).value
  }

  val benefitFormDataActionId = "RemoveBenefitFormData"

  implicit class BenefitKeyStore(keyStoreService: KeyStoreConnector) {
    def storeBenefitFormData(benefitFormData: RemoveCarBenefitFormData)(implicit hc: HeaderCarrier) = {
      keyStoreService.addKeyStoreEntry(benefitFormDataActionId, KeystoreUtils.source, keystoreKey, benefitFormData)
    }

    def storeBenefitFormData(benefitFormData: RemoveFuelBenefitFormData)(implicit hc: HeaderCarrier) = {
      keyStoreService.addKeyStoreEntry(benefitFormDataActionId, KeystoreUtils.source, keystoreKey, benefitFormData)
    }

    def loadBenefitFormData(implicit hc: HeaderCarrier) = {
      keyStoreService.getEntry[RemoveCarBenefitFormData](benefitFormDataActionId, KeystoreUtils.source, keystoreKey)
    }

    def clearBenefitFormData(implicit hc: HeaderCarrier): Unit = {
      keyStoreService.deleteKeyStore(benefitFormDataActionId, KeystoreUtils.source)
    }

  }

}
