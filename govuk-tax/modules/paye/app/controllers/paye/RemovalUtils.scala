package controllers.paye

import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.common.microservice.paye.domain.Benefit
import org.joda.time.{DateTime, LocalDate}
import uk.gov.hmrc.common.microservice.paye.domain.BenefitTypes._
import play.api.data.Form
import play.api.data.Forms._
import controllers.paye.validation.RemoveBenefitValidator._
import models.paye.{DisplayBenefit, RemoveBenefitFormData, CarFuelBenefitDates}
import uk.gov.hmrc.common.microservice.paye.domain.TaxYearData
import scala.Some
import play.api.mvc.Request
import uk.gov.hmrc.utils.TaxYearResolver
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector


object RemovalUtils {

  case class RemoveBenefitData(withdrawDate: LocalDate, revisedAmounts: Map[String, BigDecimal])

  val keystoreKey = "remove_benefit"
  private final val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  private final val dateRegex = """(\d\d\d\d-\d\d-\d\d)""".r

  def updateBenefitForm(benefitStartDate: LocalDate,
                        carBenefitWithUnremovedFuelBenefit: Boolean,
                        dates: Option[CarFuelBenefitDates],
                        now: DateTime) = Form[RemoveBenefitFormData](
    mapping(
      "withdrawDate" -> localDateMapping(Some(benefitStartDate), now.toLocalDate),
      "removeCar" -> boolean,
      "fuelRadio" -> validateFuelDateChoice(carBenefitWithUnremovedFuelBenefit),
      "fuelWithdrawDate" -> validateFuelDate(dates, Some(benefitStartDate))
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
    payeRootData.findExistingBenefit(employmentNumber, FUEL).isDefined
  }

  def hasUnremovedCarBenefit(payeRootData: TaxYearData, employmentNumber: Int): Boolean = {
    payeRootData.findExistingBenefit(employmentNumber, CAR).isDefined
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

  def storeBenefitFormData(keyStoreService: KeyStoreConnector, benefit: DisplayBenefit, benefitFormData: RemoveBenefitFormData)(implicit hc: HeaderCarrier) = {
    val id: String = genBenefitFormDataId(benefit.allBenefitsToString, benefit.benefit.taxYear, benefit.benefit.employmentSequenceNumber)
    keyStoreService.addKeyStoreEntry(id, KeystoreUtils.source, keystoreKey, benefitFormData)
  }

  def loadBenefitFormData(keyStoreService: KeyStoreConnector, benefit: DisplayBenefit)(implicit hc: HeaderCarrier) = {
    val id = genBenefitFormDataId(benefit.allBenefitsToString, benefit.benefit.taxYear, benefit.benefit.employmentSequenceNumber)
    keyStoreService.getEntry[RemoveBenefitFormData](id, KeystoreUtils.source, keystoreKey)
  }

  def clearBenefitFormData(keyStoreService: KeyStoreConnector, benefit: DisplayBenefit)(implicit hc: HeaderCarrier): Unit = {
    clearBenefitFormData(keyStoreService, benefit.allBenefitsToString, benefit.benefit.taxYear, benefit.benefit.employmentSequenceNumber)
  }

  def clearBenefitFormData(keyStoreService: KeyStoreConnector, kinds: String, year: Int, employmentSequenceNumber: Int)(implicit hc: HeaderCarrier): Unit = {
    keyStoreService.deleteKeyStore(genBenefitFormDataId(kinds, year, employmentSequenceNumber), KeystoreUtils.source)
  }

  def genBenefitFormDataId(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) =
    s"RemoveBenefitForm:$benefitTypes:$taxYear:$employmentSequenceNumber"

  def storeBenefitData(keyStoreService: KeyStoreConnector, benefit: DisplayBenefit, benefitData: RemoveBenefitData)(implicit hc: HeaderCarrier) = {
    val id: String = genBenefitDataId(benefit.allBenefitsToString, benefit.benefit.taxYear, benefit.benefit.employmentSequenceNumber)
    keyStoreService.addKeyStoreEntry(id, KeystoreUtils.source, keystoreKey, benefitData)
  }

  def loadBenefitData(keyStoreService: KeyStoreConnector, benefit: DisplayBenefit)(implicit hc: HeaderCarrier) = {
    val id = genBenefitDataId(benefit.allBenefitsToString, benefit.benefit.taxYear, benefit.benefit.employmentSequenceNumber)
    keyStoreService.getEntry[RemoveBenefitData](id, KeystoreUtils.source, keystoreKey)
  }

  def clearBenefitData(keyStoreService: KeyStoreConnector, benefit: DisplayBenefit)(implicit hc: HeaderCarrier): Unit = {
    clearBenefitData(keyStoreService, benefit.allBenefitsToString, benefit.benefit.taxYear, benefit.benefit.employmentSequenceNumber)
  }

  def clearBenefitData(keyStoreService: KeyStoreConnector, kinds: String, year: Int, employmentSequenceNumber: Int)(implicit hc: HeaderCarrier): Unit = {
    keyStoreService.deleteKeyStore(genBenefitDataId(kinds, year, employmentSequenceNumber), KeystoreUtils.source)
  }

  def genBenefitDataId(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) =
    s"RemoveBenefit:$benefitTypes:$taxYear:$employmentSequenceNumber"


}
