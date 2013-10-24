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
import org.joda.time.{DateTime, LocalDate}
import org.scalatest.TestData
import uk.gov.hmrc.utils.DateConverter
import uk.gov.hmrc.common.microservice.paye.domain.Car
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode
import uk.gov.hmrc.microservice.txqueue.{TxQueueTransaction}
import controllers.DateFieldsHelper
import java.net.URI
import uk.gov.hmrc.microservice.txqueue
import concurrent.Future

class CarBenefitHomeControllerSpec extends PayeBaseSpec with MockitoSugar with DateConverter with DateFieldsHelper {

  private lazy val controller = new CarBenefitHomeController with MockMicroServicesForTests {
    override def currentTaxYear = 2013
  }

  override protected def beforeEach(testData: TestData) {
    super.beforeEach(testData)

    controller.resetAll()
  }

  val removeFuelLinkId = "rmFuelLink"
  val removeCarLinkId = "rmCarLink"
  val addCarLinkId = "addCarLink"

  val carBenefitEmployer1 = Benefit(31, 2013, 321.42, 1, null, null, null, null, null, null,
    Some(Car(Some(new LocalDate(2012, 12, 12)), None, Some(new LocalDate(2012, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), Some(BigDecimal("12343.21")))), actions("AB123456C", 2013, 1), Map.empty)

  val fuelBenefitEmployer1 = Benefit(29, 2013, 22.22, 1, null, null, null, null, null, null,
    None, actions("AB123456C", 2013, 1), Map.empty)

  val johnDensmoresBenefitsForEmployer1 = Seq(
    carBenefitEmployer1,
    fuelBenefitEmployer1)

  val taxYear = 2013
  val employmentSeqNumber = 1

  "calling carBenefitHome" should {

    "return 500 if we cannot find a primary employment for the customer" in {
      val employments = Seq(
        Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Weyland-Yutani Corp"), employmentType = 2),
        Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None, employmentType = 2))

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, employments, Seq.empty, List.empty, List.empty)

      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(500)
    }

    "show car details for user with a company car and no fuel" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefitEmployer1), List.empty, List.empty)

      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Company car provided by Weyland-Yutani Corp"
      doc.select("#car-benefit-engine").text shouldBe "1,400cc or less"
      doc.select("#car-benefit-fuel-type").text shouldBe "Diesel"
      doc.select("#car-benefit-date-available").text shouldBe "12 December 2012"
      doc.select("#car-benefit-amount").text shouldBe "£321"
      doc.select("#fuel-benefit-amount").text shouldBe ""
      doc.select("#no-car-benefit-container").text shouldBe ""
    }

    "show car details for user with a company car and fuel benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefitsForEmployer1, List.empty, List.empty)

      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Company car provided by Weyland-Yutani Corp"
      doc.select("#car-benefit-engine").text shouldBe "1,400cc or less"
      doc.select("#car-benefit-fuel-type").text shouldBe "Diesel"
      doc.select("#car-benefit-date-available").text shouldBe "12 December 2012"
      doc.select("#car-benefit-amount").text shouldBe "£321"
      doc.select("#fuel-benefit-amount").text shouldBe "£22"
      doc.select("#no-car-benefit-container").text shouldBe ""
    }

    "show car details for user with a company car an no employer name" in new WithApplication(FakeApplication()) {
      val johnDensmoresEmploymentsWithoutName = Seq(
        Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = None, employmentType = Employment.primaryEmploymentType),
        Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None, employmentType = 1))

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmploymentsWithoutName, johnDensmoresBenefitsForEmployer1, List.empty, List.empty)

      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Company car provided by Your employer"
      doc.select("#car-benefit-engine").text shouldBe "1,400cc or less"
      doc.select("#car-benefit-fuel-type").text shouldBe "Diesel"
      doc.select("#car-benefit-date-available").text shouldBe "12 December 2012"
      doc.select("#car-benefit-amount").text shouldBe "£321"
      doc.select("#fuel-benefit-amount").text shouldBe "£22"
      doc.select("#no-car-benefit-container").text shouldBe ""
    }

    "show Add Car link for a user without a company car" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#no-car-benefit-container").text should not be ""
      doc.select("#company-name").text shouldBe ""
      doc.select("#car-benefit-engine").text shouldBe ""
      doc.select("#car-benefit-fuel-type").text shouldBe ""
      doc.select("#car-benefit-date-available").text shouldBe ""
      doc.select("#car-benefit-amount").text shouldBe ""
      doc.select("#fuel-benefit-amount").text shouldBe ""
      doc.getElementById(addCarLinkId) should not be (None)
    }

    "not show an add Car link for a user with a company car" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefitEmployer1), List.empty, List.empty)

      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(200)
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(addCarLinkId) should be (null)
    }

    "show a remove car link and not show a remove fuel link for a user who has a car without a fuel benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefitEmployer1), List.empty, List.empty)

      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(200)
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(removeFuelLinkId) should be (null)
      doc.getElementById(removeCarLinkId) should not be (null)
    }

    "show a remove car and remove fuel link for a user who has a car and fuel benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefitsForEmployer1, List.empty, List.empty)

      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(200)
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(removeFuelLinkId) should not be (null)
      doc.getElementById(removeCarLinkId) should not be (null)
    }

    "show an add car link for a user who has 2 employments and a car on the non primary employment" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefit), List.empty, List.empty)

      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(200)
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(addCarLinkId) should not be (null)
    }

    "show an add Car link for a user with a company car who has an accepted car removal transaction" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefitEmployer1), List(generateTransactionData(31, false)), List.empty)

      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(200)
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(addCarLinkId) should not be (null)
    }

    "show a remove car link and not show a remove fuel link for a user with both benefits who has an accepted fuel removal transaction" in new WithApplication(FakeApplication()) {
      removeBenefitTransactionLinkVisibilityTest(29, false, false, true)
    }

    "not show a remove car or remove fuel link for a user with both benefits who has an accepted car removal transaction" in new WithApplication(FakeApplication()) {
      removeBenefitTransactionLinkVisibilityTest(31, false, false, false)
    }

    "show a remove car link and not show a remove fuel link for a user with both benefits who has a completed fuel removal transaction" in new WithApplication(FakeApplication()) {
      removeBenefitTransactionLinkVisibilityTest(29, true, false, true)
    }

    "not show a remove car or remove fuel link for a user with both benefits who has a completed car removal transaction" in new WithApplication(FakeApplication()) {
      removeBenefitTransactionLinkVisibilityTest(31, true, false, false)
    }

    def removeBenefitTransactionLinkVisibilityTest(removeTransactionBenefitType : Int, isCompleted : Boolean, expRemoveFuelLink : Boolean, expRemoveCarLink : Boolean){
      val acceptedTransactions = if (!isCompleted) List(generateTransactionData(removeTransactionBenefitType , false)) else List.empty
      val completedTransactions = if (isCompleted) List(generateTransactionData(removeTransactionBenefitType , true)) else List.empty

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefitEmployer1, fuelBenefitEmployer1), acceptedTransactions, completedTransactions)
      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(200)
      val doc = Jsoup.parse(contentAsString(result))

      if (expRemoveFuelLink) doc.getElementById(removeFuelLinkId) should not be (null) else doc.getElementById(removeFuelLinkId) should be (null)
      if (expRemoveCarLink) doc.getElementById(removeCarLinkId) should not be (null) else doc.getElementById(removeCarLinkId) should be (null)
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

  private def generateTransactionData(benefitType : Int, isCompletedTransaction : Boolean) : TxQueueTransaction = {
    val benefitTypes = Map(29 -> "fuel", 31 -> "car")
    val acceptedStatus = txqueue.Status("ACCEPTED", None, DateTime.now())
    val createdStatus = txqueue.Status("CREATED", None, DateTime.now())
    val completedStatus = txqueue.Status("COMPLETED", None, DateTime.now())

    val statusList = if (isCompletedTransaction)
      List(completedStatus, acceptedStatus, createdStatus)
    else
      List(acceptedStatus, createdStatus)

     TxQueueTransaction(
      new URI("Test"),
      "paye",
      new URI("/auth/oid/jdensmore"),
      None,
      statusList,
      Some(List("benefits", "remove", "message.code.removeBenefits", benefitTypes(benefitType))),
      Map("employmentSequenceNumber" -> "1", "taxYear" -> "2013", "benefitTypes" -> benefitType.toString) ,
      DateTime.now(),
      DateTime.now())
  }
}