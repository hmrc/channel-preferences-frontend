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
import uk.gov.hmrc.utils.DateConverter
import uk.gov.hmrc.common.microservice.paye.domain.Car
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode
import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import controllers.DateFieldsHelper

class CarBenefitHomeControllerSpec extends PayeBaseSpec with MockitoSugar with DateConverter with DateFieldsHelper {

  private lazy val controller = new CarBenefitHomeController with MockMicroServicesForTests {
    override def currentTaxYear = 2013
  }

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

  val taxYear = 2013
  val employmentSeqNumber = 1

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
      doc.getElementById("addCarLink") should not be (None)
    }

    "not show an add Car link for a user with a company car" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefitEmployer1), List.empty, List.empty)

      val result = controller.carBenefitHomeAction(johnDensmore, FakeRequest())

      status(result) should be(200)
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById("addCarLink") should be (null)
    }

    "show a remove car link and not show a remove fuel link for a user who has a car without a fuel benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefitEmployer1), List.empty, List.empty)

      val result = controller.carBenefitHomeAction(johnDensmore, FakeRequest())

      status(result) should be(200)
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById("rmFuelLink") should be (null)
      doc.getElementById("rmCarLink") should not be (null)
    }

    "show a remove car and remove fuel link for a user who has a car and fuel benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefitsForEmployer1, List.empty, List.empty)

      val result = controller.carBenefitHomeAction(johnDensmore, FakeRequest())

      status(result) should be(200)
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById("rmFuelLink") should not be (null)
      doc.getElementById("rmCarLink") should not be (null)
    }

    "show an add car link for a user who has 2 employments and a car on the non primary employment" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefit), List.empty, List.empty)

      val result = controller.carBenefitHomeAction(johnDensmore, FakeRequest())

      status(result) should be(200)
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById("addCarLink") should not be (null)
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
}