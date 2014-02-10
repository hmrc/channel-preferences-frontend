package controllers.paye

import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.common.microservice.paye.domain.{FuelBenefit, Benefit, TaxYearData}
import org.joda.time.{Interval, DateTime, LocalDate}
import play.api.data.Form
import play.api.data.Forms._
import controllers.paye.validation.RemoveBenefitValidator._
import play.api.mvc.Request
import uk.gov.hmrc.utils.TaxYearResolver
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import models.paye.{ReplaceCarBenefitFormData, RemoveFuelBenefitFormData, CarFuelBenefitDates, RemoveCarBenefitFormData}
import scala.Some


object RemovalUtils {

  val keystoreKey = "remove_benefit"
  private final val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  private final val dateRegex = """(\d\d\d\d-\d\d-\d\d)""".r

  def updateRemoveCarBenefitForm(values: Option[RemoveCarBenefitFormDataValues],
                                 benefitStartDate: LocalDate,
                                 fuelBenefit: Option[FuelBenefit],
                                 dates: Option[CarFuelBenefitDates],
                                 now: DateTime, taxYearInterval: Interval) = Form[RemoveCarBenefitFormData](
    mapping(
      "withdrawDate" -> localDateMapping(now.toLocalDate, taxYearInterval, fuelBenefit.flatMap(_.dateWithdrawn), carBenefitMapping(Some(benefitStartDate))),
      "carUnavailable" -> validateMandatoryBoolean,
      "numberOfDaysUnavailable" -> validateNumberOfDaysUnavailable(values, benefitStartDate, taxYearInterval),
      "removeEmployeeContributes" -> validateMandatoryBoolean,
      "removeEmployeeContribution" -> validateEmployeeContribution(values),
      "fuelRadio" -> validateFuelDateChoice(fuelBenefit.exists(_.isActive)),
      "fuelWithdrawDate" -> validateFuelDate(dates, Some(benefitStartDate), taxYearInterval)
    )(RemoveCarBenefitFormData.apply)(RemoveCarBenefitFormData.unapply)
  )

  def updateRemoveFuelBenefitForm(benefitStartDate: LocalDate, now: DateTime, taxYearInterval: Interval) = Form[RemoveFuelBenefitFormData](
    mapping(
      "withdrawDate" -> localDateMapping(now.toLocalDate, taxYearInterval, None, fuelBenefitMapping(Some(benefitStartDate)))
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
    payeRootData.findActiveFuelBenefit(employmentNumber).isDefined
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

    def loadCarBenefitFormData(implicit hc: HeaderCarrier) = {
      keyStoreService.getEntry[RemoveCarBenefitFormData](benefitFormDataActionId, KeystoreUtils.source, keystoreKey)
    }

    def loadFuelBenefitFormData(implicit hc: HeaderCarrier) = {
      keyStoreService.getEntry[RemoveFuelBenefitFormData](benefitFormDataActionId, KeystoreUtils.source, keystoreKey)
    }

    def clearBenefitFormData(implicit hc: HeaderCarrier): Unit = {
      keyStoreService.deleteKeyStore(benefitFormDataActionId, KeystoreUtils.source)
    }
  }

  implicit class ReplaceBenefitKeyStore(keyStoreService: KeyStoreConnector) {
    val actionId = "ReplaceCarBenefitFormData"
    val keystoreKey = "replace_benefit"

    def storeFormData(formData: ReplaceCarBenefitFormData)(implicit hc: HeaderCarrier) = {
      keyStoreService.addKeyStoreEntry(actionId, KeystoreUtils.source, keystoreKey, formData)
    }

    def loadFormData(implicit hc: HeaderCarrier) = {
      keyStoreService.getEntry[ReplaceCarBenefitFormData](actionId, KeystoreUtils.source, keystoreKey)
    }

    def clearFormData(implicit hc: HeaderCarrier) = {
      keyStoreService.deleteKeyStore(actionId, KeystoreUtils.source)
    }
  }

}
