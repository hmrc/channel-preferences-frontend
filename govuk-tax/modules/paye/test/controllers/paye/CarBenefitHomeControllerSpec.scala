package controllers.paye

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.paye.domain._
import org.mockito.Mockito._
import org.mockito.Matchers
import uk.gov.hmrc.common.microservice.paye.domain.Employment
import org.joda.time.LocalDate
import org.scalatest.TestData
import controllers.paye.CarBenefitFormFields._
import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import scala.Some
import uk.gov.hmrc.common.microservice.paye.domain.Car
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode
import uk.gov.hmrc.utils.{DateConverter, DateTimeUtils}
import uk.gov.hmrc.common.TaxYearResolver

class CarBenefitHomeControllerSpec extends PayeBaseSpec with MockitoSugar with DateConverter {

  val now = DateTimeUtils.now.withDate(2013, 10, 3)
  val inTwoDaysTime = now.plusDays(2).toLocalDate
  val inThreeDaysTime = now.plusDays(3).toLocalDate
  val endOfTaxYearMinusOne = new LocalDate(2013, 4, 4)

  private lazy val controller = new CarBenefitHomeController(timeSource = () => now) with MockMicroServicesForTests

  override protected def beforeEach(testData: TestData) {
    super.beforeEach(testData)

    controller.resetAll()
  }

  val carBenefitEmployer1 = Benefit(benefitType = 31, taxYear = 2013, grossAmount = 321.42, employmentSequenceNumber = 1, null, null, null, null, null, null,
    car = Some(Car(Some(new LocalDate(2012, 12, 12)), None, Some(new LocalDate(2012, 12, 12)), 0, 2, 124, 1, "B", BigDecimal("12343.21"))), actions("AB123456C", 2013, 1), Map.empty)

  val fuelBenefitEmployer1 = Benefit(benefitType = 29, taxYear = 2013, grossAmount = 22.22, employmentSequenceNumber = 1, null, null, null, null, null, null,
    car = None, actions("AB123456C", 2013, 1), Map.empty)

  val johnDensmoresBenefitsForEmployer1 = Seq(
    carBenefitEmployer1,
    fuelBenefitEmployer1)

  "calling carBenefitHome" should {

    "return 500 if we cannot find a primary employment for the customer" in {
      val employments = Seq(
        Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Weyland-Yutani Corp"), 2),
        Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None, 2))

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, employments, Seq.empty, List.empty, List.empty)

      val result = controller.carBenefitHomeAction(johnDensmore, FakeRequest())

      status(result) should be(500)
    }

    "show car details for user with a company car and no fuel" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefitEmployer1), List.empty, List.empty)

      val result = controller.carBenefitHomeAction(johnDensmore, FakeRequest())

      status(result) should be(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Weyland-Yutani Corp"
      doc.select("#car-benefit-engine").text shouldBe "0-1400 cc"
      doc.select("#car-benefit-fuel-type").text shouldBe "Bi-Fuel"
      doc.select("#car-benefit-date-available").text shouldBe "December 12, 2012"
      doc.select("#car-benefit-amount").text shouldBe "£321.42"
      doc.select("#fuel-benefit-amount").text shouldBe ""
      doc.select("#no-car-benefit-container").text shouldBe ""
    }

    "show car details for user with a company car and fuel benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefitsForEmployer1, List.empty, List.empty)

      val result = controller.carBenefitHomeAction(johnDensmore, FakeRequest())

      status(result) should be(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Weyland-Yutani Corp"
      doc.select("#car-benefit-engine").text shouldBe "0-1400 cc"
      doc.select("#car-benefit-fuel-type").text shouldBe "Bi-Fuel"
      doc.select("#car-benefit-date-available").text shouldBe "December 12, 2012"
      doc.select("#car-benefit-amount").text shouldBe "£321.42"
      doc.select("#fuel-benefit-amount").text shouldBe "£22.22"
      doc.select("#no-car-benefit-container").text shouldBe ""
    }

    "show car details for user with a company car an no employer name" in new WithApplication(FakeApplication()) {
      val johnDensmoresEmploymentsWithoutName = Seq(
        Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = None, Employment.primaryEmploymentType),
        Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None, 1))

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmploymentsWithoutName, johnDensmoresBenefitsForEmployer1, List.empty, List.empty)

      val result = controller.carBenefitHomeAction(johnDensmore, FakeRequest())

      status(result) should be(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Your employer"
      doc.select("#car-benefit-engine").text shouldBe "0-1400 cc"
      doc.select("#car-benefit-fuel-type").text shouldBe "Bi-Fuel"
      doc.select("#car-benefit-date-available").text shouldBe "December 12, 2012"
      doc.select("#car-benefit-amount").text shouldBe "£321.42"
      doc.select("#fuel-benefit-amount").text shouldBe "£22.22"
      doc.select("#no-car-benefit-container").text shouldBe ""
    }

    "show Add Car link for a user without a company car" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      val result = controller.carBenefitHomeAction(johnDensmore, FakeRequest())

      status(result) should be(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#no-car-benefit-container").text should not be ""
      doc.select("#company-name").text shouldBe ""
      doc.select("#car-benefit-engine").text shouldBe ""
      doc.select("#car-benefit-fuel-type").text shouldBe ""
      doc.select("#car-benefit-date-available").text shouldBe ""
      doc.select("#car-benefit-amount").text shouldBe ""
      doc.select("#fuel-benefit-amount").text shouldBe ""
    }

  }

  "calling start add car benefit" should {
    "return 200 and show the add car benefit form with the employers name  " in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      val result = controller.startAddCarBenefitAction(johnDensmore, FakeRequest(), 2013, 1)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Weyland-Yutani Corp"
    }
    "return 200 and show the add car benefit form and show your company if the employer name does not exist " in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      val result = controller.startAddCarBenefitAction(johnDensmore, FakeRequest(), 2013, 2)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Your employer"
    }
    "return 400 when employer for sequence number does not exist" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      val result = controller.startAddCarBenefitAction(johnDensmore, FakeRequest(), 2013, 5)

      status(result) shouldBe 400
    }
  }

  "submitting add car benefit" should {


    "return 200 when form data validates successfully" in {
      assertSuccessSubmit(Some(inTwoDaysTime), false,  None, false,  None)
      assertSuccessSubmit(Some(inTwoDaysTime), true,  Some(15), false,  None)
      assertSuccessSubmit(Some(inTwoDaysTime), false,  None, true,  Some(endOfTaxYearMinusOne))
      assertSuccessSubmit(None, false,  None, false,  None)
      assertSuccessSubmit(None, false,  None, true,  Some(endOfTaxYearMinusOne))
      assertSuccessSubmit(None, true,  Some(30), true,  Some(endOfTaxYearMinusOne))
    }

    def assertSuccessSubmit(providedFromVal : Option[LocalDate],
                                       carUnavailableVal:  Boolean,
                                       numberOfDaysUnavailableVal: Option[Int],
                                       giveBackThisTaxYearVal: Boolean,
                                       providedToVal: Option[LocalDate]) {
      val result = controller.saveAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(
        providedFromVal, carUnavailableVal, numberOfDaysUnavailableVal,
        giveBackThisTaxYearVal, providedToVal))

      //todo assert saved entity / keystore capture (e.g. defaults for optional dates)

      status(result) shouldBe 200
    }

  }

  private def setupMocksForJohnDensmore(taxCodes: Seq[TaxCode], employments: Seq[Employment], benefits: Seq[Benefit],
                                        acceptedTransactions: List[TxQueueTransaction], completedTransactions: List[TxQueueTransaction]) {
    when(controller.payeMicroService.linkedResource[Seq[TaxCode]]("/paye/AB123456C/tax-codes/2013")).thenReturn(Some(taxCodes))
    when(controller.payeMicroService.linkedResource[Seq[Employment]]("/paye/AB123456C/employments/2013")).thenReturn(Some(employments))
    when(controller.payeMicroService.linkedResource[Seq[Benefit]]("/paye/AB123456C/benefits/2013")).thenReturn(Some(benefits))
    when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/ACCEPTED/.*"))).thenReturn(Some(acceptedTransactions))
    when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/COMPLETED/.*"))).thenReturn(Some(completedTransactions))
  }


  private def newRequestForSaveAddCarBenefit(providedFromVal : Option[LocalDate] = Some(inTwoDaysTime),
                                       carUnavailableVal:  Boolean = false,
                                       numberOfDaysUnavailableVal: Option[Int] = Some(10) ,
                                       giveBackThisTaxYearVal: Boolean = true,
                                       providedToVal: Option[LocalDate] = Some(inThreeDaysTime)) =

        FakeRequest().withFormUrlEncodedBody(providedFrom -> providedFromVal.map(formatToString(_)).getOrElse(""),
          carUnavailable -> carUnavailableVal.toString,
          numberOfDaysUnavailable -> numberOfDaysUnavailableVal.getOrElse("").toString,
          giveBackThisTaxYear -> giveBackThisTaxYearVal.toString,
          providedTo -> providedToVal.map(formatToString(_)).getOrElse(""))

}
