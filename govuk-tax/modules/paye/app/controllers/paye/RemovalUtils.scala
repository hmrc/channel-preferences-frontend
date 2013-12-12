package controllers.paye

import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.common.microservice.paye.domain.Benefit
import org.joda.time.{Interval, DateTime, LocalDate}
import uk.gov.hmrc.common.microservice.paye.domain.BenefitTypes._
import play.api.data.Form
import play.api.data.Forms._
import controllers.paye.validation.RemoveBenefitValidator._
import models.paye.{RemoveBenefitFormData, CarFuelBenefitDates}
import uk.gov.hmrc.common.microservice.paye.domain.TaxYearData
import scala.Some
import play.api.mvc.Request
import uk.gov.hmrc.utils.TaxYearResolver
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import scala.concurrent.Future


object RemovalUtils {

  case class RemoveBenefitData(withdrawDate: LocalDate, revisedAmounts: Map[String, BigDecimal])


  val keystoreKey = "remove_benefit"
  private final val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  private final val dateRegex = """(\d\d\d\d-\d\d-\d\d)""".r

  def updateBenefitForm(benefitStartDate: LocalDate,
                        carBenefitWithUnremovedFuelBenefit: Boolean,
                        dates: Option[CarFuelBenefitDates],
                        now: DateTime, taxYearInterval:Interval) = Form[RemoveBenefitFormData](
    mapping(
      "withdrawDate" -> localDateMapping(Some(benefitStartDate), now.toLocalDate, taxYearInterval),
      "fuelRadio" -> validateFuelDateChoice(carBenefitWithUnremovedFuelBenefit),
      "fuelWithdrawDate" -> validateFuelDate(dates, Some(benefitStartDate), taxYearInterval)
    )(RemoveBenefitFormData.apply)(RemoveBenefitFormData.unapply)
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
  val benefitDataActionId = "RemoveBenefitData"

  implicit class BenefitKeyStore(keyStoreService: KeyStoreConnector) {
    def storeBenefitFormData(benefitFormData: RemoveBenefitFormData)(implicit hc: HeaderCarrier) = {
      keyStoreService.addKeyStoreEntry(benefitFormDataActionId, KeystoreUtils.source, keystoreKey, benefitFormData)
    }

    def loadBenefitFormData(implicit hc: HeaderCarrier) = {
      keyStoreService.getEntry[RemoveBenefitFormData](benefitFormDataActionId, KeystoreUtils.source, keystoreKey)
    }

    def clearBenefitFormData(implicit hc: HeaderCarrier): Unit = {
      keyStoreService.deleteKeyStore(benefitFormDataActionId, KeystoreUtils.source)
    }


    def storeBenefitData(benefitData: RemoveBenefitData)(implicit hc: HeaderCarrier) = {
      keyStoreService.addKeyStoreEntry(benefitDataActionId, KeystoreUtils.source, keystoreKey, benefitData)
    }

    def loadBenefitData(implicit hc: HeaderCarrier): Future[Option[RemoveBenefitData]] = {
      keyStoreService.getEntry[RemoveBenefitData](benefitDataActionId, KeystoreUtils.source, keystoreKey)
    }


    def clearBenefitData(implicit hc: HeaderCarrier): Unit = {
      keyStoreService.deleteKeyStore(benefitDataActionId, KeystoreUtils.source)
    }
  }

}
