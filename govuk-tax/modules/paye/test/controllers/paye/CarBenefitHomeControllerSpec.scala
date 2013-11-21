package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain._
import BenefitTypes._
import org.scalatest.mock.MockitoSugar
import play.api.test.{WithApplication, FakeRequest, FakeApplication}
import play.api.test.Helpers._
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.paye.domain._
import org.mockito.Mockito._
import org.mockito.{Mockito, Matchers}
import org.joda.time.LocalDate
import org.scalatest.TestData
import uk.gov.hmrc.utils.DateConverter
import controllers.DateFieldsHelper
import concurrent.Future
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
import controllers.common.actions.HeaderCarrier

class CarBenefitHomeControllerSpec extends PayeBaseSpec with MockitoSugar with DateConverter with DateFieldsHelper {

  val mockPayeConnector = mock[PayeConnector]
  val mockTxQueueConnector = mock[TxQueueConnector]
  val mockAuditConnector = mock[AuditConnector]
  val mockAuthConnector = mock[AuthConnector]

  private lazy val controller = new CarBenefitHomeController(mockAuditConnector, mockAuthConnector)(mockPayeConnector, mockTxQueueConnector){
    override def currentTaxYear = 2013
  }

  override protected def beforeEach(testData: TestData) {
    super.beforeEach(testData)

    Mockito.reset(mockPayeConnector)
    Mockito.reset(mockTxQueueConnector)
    Mockito.reset(mockAuditConnector)
    Mockito.reset(mockAuthConnector)
  }

  val removeFuelLinkId = "rmFuelLink"
  val removeCarLinkId = "rmCarLink"
  val addCarLinkId = "addCarLink"
  val addFuelLinkId = "addFuelLink"
  val taxYear = 2013
  val employmentSeqNumber = 1

  "calling carBenefitHome" should {

    "return 500 if we cannot find a primary employment for the customer and show an error page" in new WithApplication(FakeApplication()) {
      val employments = Seq(
        Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Weyland-Yutani Corp"), employmentType = 2),
        Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None, employmentType = 2))

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, employments, Seq.empty, List.empty, List.empty)

      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(500)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#error-message") should not be empty
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
      doc.select("#private-fuel").text shouldBe "No"
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
      doc.select("#private-fuel").text shouldBe "Yes, private fuel is available when you use the car"
    }

    "show car details for a user with a company car and a fuel benefit that has been withdrawn" in new WithApplication(FakeApplication()) {
      val benefits = Seq(
        carBenefitEmployer1,
        fuelBenefitEmployer1.copy(dateWithdrawn = Some(new LocalDate(2013,6,6))))
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, benefits, List.empty, List.empty)

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
      doc.select("#private-fuel").text shouldBe "Weyland-Yutani Corp did pay for fuel for private travel, but stopped paying on 6 June 2013"
    }

    "show car details for user with a company car where the employer name is unknown" in new WithApplication(FakeApplication()) {
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

    "show an Add Car link for a user without a company car and do not show the add fuel link" in new WithApplication(FakeApplication()) {

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
      doc.getElementById(addCarLinkId) should not be (null)
      doc.getElementById(addFuelLinkId) shouldBe (null)
    }

    "show an Add Fuel link for a user with a car benefit but no fuel benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefitEmployer1), List.empty, List.empty)

      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text should include(johnDensmoresEmployments(0).employerName.get)
      doc.select("#car-benefit-fuel-type").text should not be empty
      doc.select("#car-benefit-fuel-type").text.toLowerCase shouldBe carBenefitEmployer1.car.get.fuelType.get
      doc.getElementById(addFuelLinkId) should not be (null)
      doc.getElementById(addCarLinkId) shouldBe (null)
    }

    "not show an add Car or add Fuel links for a user with company car and fuel benefits" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefitEmployer1, fuelBenefitEmployer1), List.empty, List.empty)

      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(200)
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(addCarLinkId) should be (null)
      doc.getElementById(addFuelLinkId) should be (null)
    }

    "not show an Add Fuel Link if the user has a company car with fuel of type electricicity" in new WithApplication(FakeApplication()) {
      val electricCarBenefit = Benefit(31, testTaxYear, 321.42, 1, None, None, None, None, None, None, None,
        Some(Car(Some(new LocalDate(testTaxYear - 1, 12, 12)), None, Some(new LocalDate(testTaxYear - 1, 12, 12)), Some(0), Some("electricity"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), actions("AB123456C", testTaxYear, 1), Map.empty)

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(electricCarBenefit), List.empty, List.empty)
      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(addFuelLinkId) shouldBe null
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

    "show an add Car link for a user without a company car" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(200)
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(addCarLinkId) should not be (null)
    }


    "not show a remove car or remove fuel link for a user with no car benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)
      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(200)
      val doc = Jsoup.parse(contentAsString(result))

      doc.getElementById(removeFuelLinkId) should be (null)
      doc.getElementById(removeCarLinkId) should be (null)
    }

    "show a remove car link and not show a remove fuel link for a user with a car benefit but no fuel benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefitEmployer1), List.empty, List.empty)
      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))

      status(result) should be(200)
      val doc = Jsoup.parse(contentAsString(result))

      doc.getElementById(removeFuelLinkId) should be (null)
      doc.getElementById(removeCarLinkId) should not be null
    }

    "show an add company car benefit link if for a user who has no car benefit " in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)
      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(addCarLinkId).text should include("add a company car")
    }

    "do not show an add company car benefit link if the user has a car benefit" in new WithApplication(FakeApplication()) {
      val benefits = Seq[Benefit](carBenefitEmployer1)
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, benefits, List.empty, List.empty)
      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(addCarLinkId) shouldBe null
    }

    "display recent transactions for John Densmore" in new WithApplication(FakeApplication()) {

      val (employerName1, employerName2) = ("Johnson PLC", "Xylophone and Son Ltd")
      val johnDensmoresEmployments = Seq(
        Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some(employerName1), employmentType = primaryEmploymentType),
        Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = Some(employerName2), employmentType = 2))
      val testTransactions = List(removedCarTransaction, otherTransaction, removedFuelTransaction, addCarTransaction, addFuelTransaction, removedFuelTransactionForEmployment2)

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, testTransactions, testTransactions)

      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))
      val doc = Jsoup.parse(contentAsString(result))
      val recentChanges = doc.select(".overview__actions__done").text
      recentChanges should include(s"On 2 December 2012, you removed your company car benefit from $employerName1. This is being processed and you will receive a new Tax Code within 2 days.")
      recentChanges should include(s"On 2 December 2012, you removed your company car benefit from $employerName1. This has been processed and your new Tax Code is 430L. $employerName1 have been notified.")

      recentChanges should include(s"On 2 December 2012, you removed your company fuel benefit from $employerName1. This is being processed and you will receive a new Tax Code within 2 days.")
      recentChanges should include(s"On 2 December 2012, you removed your company fuel benefit from $employerName1. This has been processed and your new Tax Code is 430L. $employerName1 have been notified.")

      recentChanges should include(s"On 2 December 2012, you added your company fuel benefit from $employerName1. This is being processed and you will receive a new Tax Code within 2 days.")
      recentChanges should include(s"On 2 December 2012, you added your company fuel benefit from $employerName1. This has been processed and your new Tax Code is 430L. $employerName1 have been notified.")
      recentChanges should not include employerName2
      doc.select(".no_actions") shouldBe empty
    }

    "display recent transactions for John Densmore when both car and fuel benefit have been removed and added " in new WithApplication(FakeApplication()) {
      val removeCar1AndFuel1CompletedTransaction = transactionWithTags(List("paye", "test", "message.code.removeBenefits"), Map("benefitTypes" -> s"$CAR,$FUEL"))
      val addCar2AndFuel2CompletedTransaction = transactionWithTags(List("paye", "test", "message.code.addBenefits"), Map("benefitTypes" -> s"$CAR,$FUEL"))
      val removeCar2AndFuel2AcceptedTransaction = transactionWithTags(List("paye", "test", "message.code.removeBenefits"), Map("benefitTypes" -> s"$CAR,$FUEL"))
      val addCar3AndFuel4AcceptedTransaction = transactionWithTags(List("paye", "test", "message.code.addBenefits"), Map("benefitTypes" -> s"$CAR,$FUEL"))

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List(removeCar2AndFuel2AcceptedTransaction, addCar3AndFuel4AcceptedTransaction), List(removeCar1AndFuel1CompletedTransaction, addCar2AndFuel2CompletedTransaction))


      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))
      val doc = Jsoup.parse(contentAsString(result))
      val recentChanges = doc.select(".overview__actions__done").text
      recentChanges should include(s"On 2 December 2012, you added your company car and fuel benefit from Weyland-Yutani Corp. This is being processed and you will receive a new Tax Code within 2 days.")
      recentChanges should include(s"2 December 2012, you removed your company car and fuel benefit from Weyland-Yutani Corp.")
      recentChanges should include(s"On 2 December 2012, you added your company car and fuel benefit from Weyland-Yutani Corp. This has been processed and your new Tax Code is 430L. Weyland-Yutani Corp have been notified.")
      recentChanges should include(s"2 December 2012, you removed your company car and fuel benefit from Weyland-Yutani Corp. This has been processed and your new Tax Code is 430L. Weyland-Yutani Corp have been notified.")
    }

    "display a suitable message for John Densmore if there are no transactions" in new WithApplication(FakeApplication()) {

     setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val result = Future.successful(controller.carBenefitHomeAction(johnDensmore, FakeRequest()))
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".no_actions") should not be empty
      doc.select(".no_actions").text should include("no changes")
    }
  }

  private def setupMocksForJohnDensmore(taxCodes: Seq[TaxCode], employments: Seq[Employment], benefits: Seq[Benefit],
                                        acceptedTransactions: List[TxQueueTransaction], completedTransactions: List[TxQueueTransaction]) {
    implicit val hc = HeaderCarrier()
    when(mockPayeConnector.linkedResource[Seq[TaxCode]]("/paye/AB123456C/tax-codes/2013")).thenReturn(Some(taxCodes))
    when(mockPayeConnector.linkedResource[Seq[Employment]]("/paye/AB123456C/employments/2013")).thenReturn(Some(employments))
    when(mockPayeConnector.linkedResource[Seq[Benefit]]("/paye/AB123456C/benefits/2013")).thenReturn(Some(benefits))
    when(mockTxQueueConnector.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/ACCEPTED/.*"))(Matchers.eq(hc))).thenReturn(Some(acceptedTransactions))
    when(mockTxQueueConnector.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/COMPLETED/.*"))(Matchers.eq(hc))).thenReturn(Some(completedTransactions))
  }
}