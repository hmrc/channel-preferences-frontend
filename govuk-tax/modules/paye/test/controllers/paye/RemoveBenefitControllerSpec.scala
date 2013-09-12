package controllers.paye

import org.scalatest.mock.MockitoSugar
import controllers.common.CookieEncryption
import org.scalatest.TestData
import org.joda.time.LocalDate
import play.api.test.{ WithApplication, FakeRequest }
import views.formatting.Dates
import play.api.test.Helpers._
import uk.gov.hmrc.microservice.paye.PayeMicroService
import org.mockito.Mockito._
import org.mockito.{ ArgumentMatcher, Matchers }
import uk.gov.hmrc.microservice.paye.domain._
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.microservice.paye.CalculationResult
import uk.gov.hmrc.microservice.paye.domain.Employment
import play.api.test.FakeApplication
import uk.gov.hmrc.microservice.paye.domain.Benefit
import uk.gov.hmrc.microservice.paye.domain.TaxCode
import uk.gov.hmrc.microservice.domain.{ RegimeRoots, User }
import org.jsoup.Jsoup

class RemoveBenefitControllerSpec extends PayeBaseSpec with MockitoSugar with CookieEncryption {

  import models.paye.BenefitTypes._

  private lazy val controller = new RemoveBenefitController with MockMicroServicesForTests

  override protected def beforeEach(testData: TestData) {
    super.beforeEach(testData)

    controller.resetAll()
  }

  val isBenefitOfType = (benefType: Int) => new ArgumentMatcher[Benefit] {
    def matches(benefit: Any) = benefit != null && benefit.asInstanceOf[Benefit].benefitType == benefType
  }

  private def setupMocksForJohnDensmore(taxCodes: Seq[TaxCode], employments: Seq[Employment], benefits: Seq[Benefit],
    acceptedTransactions: List[TxQueueTransaction], completedTransactions: List[TxQueueTransaction]) {
    when(controller.payeMicroService.linkedResource[Seq[TaxCode]]("/paye/AB123456C/tax-codes/2013")).thenReturn(Some(taxCodes))
    when(controller.payeMicroService.linkedResource[Seq[Employment]]("/paye/AB123456C/employments/2013")).thenReturn(Some(employments))
    when(controller.payeMicroService.linkedResource[Seq[Benefit]]("/paye/AB123456C/benefits/2013")).thenReturn(Some(benefits))
    when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/ACCEPTED/.*"))).thenReturn(Some(acceptedTransactions))
    when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/COMPLETED/.*"))).thenReturn(Some(completedTransactions))
  }

  "Removing FUEL benefit only" should {

    "notify the user the fuel benefit will be removed" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)
      val result = controller.benefitRemovalFormAction(johnDensmore, FakeRequest(), FUEL.toString, 2013, 2)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".amount").text shouldBe "£22.22"
      doc.select("label.checkbox").text should include("no longer provide me with this benefit")
    }
  }


  "The car benefit removal method" should {
    "in step 1, notify the user that the fuel benefit is going to be removed with the car benefit when removing car benefit" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val result = controller.benefitRemovalFormAction(johnDensmore, FakeRequest(), "31", 2013, 2)
      status(result) shouldBe 200
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include("remove your fuel benefit")

    }

    "in step 1, not notify the user about fuel benefit if the user does not have one" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefit), List.empty, List.empty)

      val result = controller.benefitRemovalFormAction(johnDensmore, FakeRequest(), "31", 2013, 2)
      status(result) shouldBe 200
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should not include ("remove your fuel benefit")

    }

    "in step 2, display the calculated value for removing both fuel and car benefit if the user chose to remove the car benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      def requestBenefitRemovalFormSubmission(date: Option[LocalDate], agreed: Boolean, removeFuel: Boolean) =
        FakeRequest().withFormUrlEncodedBody("withdrawDate" -> date.map(Dates.shortDate(_)).getOrElse(""), "agreement" -> agreed.toString.toLowerCase)

      val carCalculationResult = CalculationResult(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(controller.payeMicroService.calculateWithdrawBenefit(Matchers.argThat(isBenefitOfType(31)), Matchers.any[LocalDate]())).thenReturn(carCalculationResult)

      val fuelCalculationResult = CalculationResult(Map("2013" -> BigDecimal(10.01), "2014" -> BigDecimal(0)))
      when(controller.payeMicroService.calculateWithdrawBenefit(Matchers.argThat(isBenefitOfType(29)), Matchers.any[LocalDate]())).thenReturn(fuelCalculationResult)

      val withdrawDate = new LocalDate()
      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, true), "31", 2013, 2)

      status(result) shouldBe 200
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company car and fuel")
      requestBenefits should include regex "Personal Allowance by.*£210.17.".r
    }

    "in step 2, display the calculated value for removing car benefit only if the user do not have fuel benefit for the same employment" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, carAndFuelBenefitWithDifferentEmploymentNumbers, List.empty, List.empty)

      def requestBenefitRemovalFormSubmission(date: Option[LocalDate], agreed: Boolean, removeFuel: Boolean) =
        FakeRequest().withFormUrlEncodedBody("withdrawDate" -> date.map(Dates.shortDate(_)).getOrElse(""), "agreement" -> agreed.toString.toLowerCase)

      val carCalculationResult = CalculationResult(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(controller.payeMicroService.calculateWithdrawBenefit(Matchers.argThat(isBenefitOfType(31)), Matchers.any[LocalDate]())).thenReturn(carCalculationResult)

      val withdrawDate = new LocalDate()
      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, false), "31", 2013, 2)

      status(result) shouldBe 200
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company car")
      requestBenefits should not include ("and fuel")
      requestBenefits should include regex "Personal Allowance by.*£197.96.".r
    }

    "in step 2, request removal for both fuel and car benefit when both benefits are selected and user confirms" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      when(controller.payeMicroService.removeBenefits(Matchers.any[String], Matchers.any[String](), Matchers.any[Int](), Matchers.any[Seq[Benefit]](), Matchers.any[LocalDate](), Matchers.any[BigDecimal]())).thenReturn(Some(TransactionId("someIdForCarAndFuelRemoval")))

      val withdrawDate = new LocalDate(2013, 7, 18)
      when(controller.keyStoreMicroService.getEntry[RemoveBenefitData](johnDensmore.oid, "paye_ui", "remove_benefit")).thenReturn(Some(RemoveBenefitData(withdrawDate, "210.17")))

      val result = controller.confirmBenefitRemovalAction(johnDensmore, FakeRequest(), "31,29", 2013, 2)

      verify(controller.payeMicroService, times(1)).removeBenefits("/paye/AB123456C/benefits/2013/1/remove/31", "AB123456C", 22, Seq(carBenefit, fuelBenefit), withdrawDate, BigDecimal("210.17")) //Not url expected

      status(result) shouldBe 303

      headers(result).get("Location") shouldBe Some("/benefits/31,29/confirmation/someIdForCarAndFuelRemoval")
    }

    "in step 3, display page for confirmation of removal of both fuel and car benefit when both benefits are selected and user confirms" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val transaction: TxQueueTransaction = mock[TxQueueTransaction]
      when(controller.txQueueMicroService.transaction(Matchers.eq("210"), Matchers.any[PayeRoot])).thenReturn(Some(transaction))

      val withdrawDate = new LocalDate(2013, 7, 18)
      when(controller.keyStoreMicroService.getEntry[RemoveBenefitData](johnDensmore.oid, "paye_ui", "remove_benefit")).thenReturn(Some(RemoveBenefitData(withdrawDate, "210.17")))

      val result = controller.benefitRemovedAction(johnDensmore, FakeRequest(), "31,29", "210")

      status(result) shouldBe 200
      val requestBenefits = contentAsString(result)
      requestBenefits should include("210")
      requestBenefits should include("car and fuel benefit removed")

    }
  }

  "The remove benefit method" should {

    def requestBenefitRemovalFormSubmission(date: Option[LocalDate], agreed: Boolean) =
      FakeRequest().withFormUrlEncodedBody("withdrawDate" -> date.map(Dates.shortDate(_)).getOrElse(""), "agreement" -> agreed.toString.toLowerCase)

    "in step 1 display car details" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val result = controller.benefitRemovalFormAction(johnDensmore, FakeRequest(), "31", 2013, 2)
      status(result) shouldBe 200
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£321.42".r
    }

    "in step 1 display an error message when return date of car greater than 7 days" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val invalidWithdrawDate = new LocalDate().plusDays(36)
      val result = controller.requestBenefitRemovalAction(johnDensmore,
        requestBenefitRemovalFormSubmission(Some(invalidWithdrawDate), true), "31", 2013, 2)

      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£321.42".r
      requestBenefits should include("Invalid date: Return date cannot be greater than 7 days from today")
    }

    "in step 1 display an error message when return date of the car is in the previous tax year" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val invalidWithdrawDate = new LocalDate(1999, 2, 1)
      val result = controller.requestBenefitRemovalAction(johnDensmore,
        requestBenefitRemovalFormSubmission(Some(invalidWithdrawDate), true), "31", 2013, 2)

      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£321.42".r
      requestBenefits should include("Invalid date: Return date cannot be in previous tax years")
    }

    "in step 1 display an error message when return date of the car is in the next tax year" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val invalidWithdrawDate = new LocalDate(2030, 2, 1)
      val result = controller.requestBenefitRemovalAction(johnDensmore,
        requestBenefitRemovalFormSubmission(Some(invalidWithdrawDate), true), "31", 2013, 2)

      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£321.42".r
      requestBenefits should include("Invalid date: Return date cannot be in next tax years")
    }

    "in step 1 display an error message when return date is not set" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(None, true), "31", 2013, 2)

      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£321.42".r
      requestBenefits should include("Invalid date: Use format DD/MM/YYYY, e.g. 01/12/2013")
    }

    "in step 1 display an error message when agreement checkbox is not selected" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val result = controller.requestBenefitRemovalAction(johnDensmore, FakeRequest().withFormUrlEncodedBody("withdrawDate" -> ""), "31", 2013, 2)

      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£321.42".r
      requestBenefits should include("Invalid date: Use format DD/MM/YYYY, e.g. 01/12/2013")
    }

    "in step 2 display the calculated value" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefit), List.empty, List.empty)

      val calculationResult = CalculationResult(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(controller.payeMicroService.calculateWithdrawBenefit(Matchers.any[Benefit](), Matchers.any[LocalDate]())).thenReturn(calculationResult)

      val withdrawDate = new LocalDate()
      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true), "31", 2013, 2)

      status(result) shouldBe 200
      val requestBenefits = contentAsString(result)
      requestBenefits should include regex "Personal Allowance by.*£197.96.".r
    }

    "in step 2 save the withdrawDate to the keystore" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefit), List.empty, List.empty)

      val revisedAmount = BigDecimal(123.46)
      val withdrawDate = new LocalDate()

      val calculationResult = CalculationResult(Map("2013" -> revisedAmount, "2014" -> BigDecimal(0)))
      when(controller.payeMicroService.calculateWithdrawBenefit(Matchers.any[Benefit](), Matchers.any[LocalDate]())).thenReturn(calculationResult)

      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true), "31", 2013, 2)

      verify(controller.keyStoreMicroService, times(1)).addKeyStoreEntry(johnDensmore.oid, "paye_ui", "remove_benefit", RemoveBenefitData(withdrawDate, "123.46"))
    }

    "in step 2 call the paye service to remove the benefit and render the success page" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      when(controller.payeMicroService.removeBenefits(Matchers.any[String], Matchers.any[String](), Matchers.any[Int](), Matchers.any[Seq[Benefit]](), Matchers.any[LocalDate](), Matchers.any[BigDecimal]())).thenReturn(Some(TransactionId("someId")))

      val withdrawDate = new LocalDate(2013, 7, 18)
      when(controller.keyStoreMicroService.getEntry[RemoveBenefitData](johnDensmore.oid, "paye_ui", "remove_benefit")).thenReturn(Some(RemoveBenefitData(withdrawDate, "123.45")))

      val result = controller.confirmBenefitRemovalAction(johnDensmore, FakeRequest(), "31", 2013, 2)

      verify(controller.payeMicroService, times(1)).removeBenefits("/paye/AB123456C/benefits/2013/1/remove/31", "AB123456C", 22, Seq(carBenefit), withdrawDate, BigDecimal("123.45"))

      status(result) shouldBe 303

      headers(result).get("Location") shouldBe Some("/benefits/31/confirmation/someId")

    }

    "in step 3 show the transaction id only if the transaction exists" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val transaction: TxQueueTransaction = mock[TxQueueTransaction]
      when(controller.txQueueMicroService.transaction(Matchers.eq("123"), Matchers.any[PayeRoot])).thenReturn(Some(transaction))

      val withdrawDate = new LocalDate(2013, 7, 18)
      when(controller.keyStoreMicroService.getEntry[RemoveBenefitData](johnDensmore.oid, "paye_ui", "remove_benefit")).thenReturn(Some(RemoveBenefitData(withdrawDate, "123.45")))

      val result = controller.benefitRemovedAction(johnDensmore, FakeRequest(), "31", "123")

      status(result) shouldBe 200
      contentAsString(result) should include("123")

    }

    "in step 3 return 404 if the transaction does not exist" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      when(controller.txQueueMicroService.transaction(Matchers.eq("123"), Matchers.any[PayeRoot])).thenReturn(None)
      when(controller.keyStoreMicroService.getEntry[RemoveBenefitData](johnDensmore.oid, "paye_ui", "remove_benefit")).thenReturn(Some(RemoveBenefitData(withdrawDate, "555")))

      val withdrawDate = new LocalDate(2013, 7, 18)
      val result = controller.benefitRemovedAction(johnDensmore, FakeRequest().withSession("withdraw_date" -> Dates.shortDate(withdrawDate)), "31", "123")

      status(result) shouldBe 404

    }

    "return the updated benefits list page if the user has gone back in the browser and resubmitted and the benefit has already been removed" in {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List(removedCarTransaction), List.empty)

      val result = controller.benefitRemovalFormAction(johnDensmore, FakeRequest(), "31", 2013, 1)
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/benefits")
    }

    "return the benefits list page if the user modifies the url to include a benefit type that they can not remove" in {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List(removedCarTransaction), List.empty)

      val result = controller.benefitRemovalFormAction(johnDensmore, FakeRequest(), "30", 2013, 1)
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/benefits")
    }

    "return to the benefits list page if the user modifies the url to include an incorrect sequence number" in {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List(removedCarTransaction), List.empty)

      val result = controller.benefitRemovalFormAction(johnDensmore, FakeRequest(), "31", 2013, 3)
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/benefits")
    }
  }

  "benefitRemoved" should {
    "render a view with correct elements" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val car = Car(None, None, None, BigDecimal(10), 1, 1, 1, "12000", BigDecimal("1432"))

      val payeRoot = new PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map()) {
        override def employments(taxYear: Int)(implicit payeMicroService: PayeMicroService): Seq[Employment] = { Seq(Employment(1, new LocalDate(), Some(new LocalDate()), "123", "123123", None)) }
        override def benefits(taxYear: Int)(implicit payeMicroService: PayeMicroService): Seq[Benefit] = { Seq(Benefit(31, 2013, BigDecimal("3"), 1, BigDecimal("4"), BigDecimal("5"), BigDecimal("6"), BigDecimal("7"), BigDecimal("8"), "payment", Some(car), Map[String, String](), Map[String, String]())) }
      }

      val user = User("wshakespeare", null, RegimeRoots(Some(payeRoot), None, None), None, None)

      val request = FakeRequest().withFormUrlEncodedBody("withdrawDate" -> "2013-07-13", "agreement" -> "true")

      when(controller.payeMicroService.calculateWithdrawBenefit(Matchers.any[Benefit], Matchers.any[LocalDate])).thenReturn(CalculationResult(Map("2013" -> BigDecimal("123"))))

      val result = controller.requestBenefitRemovalAction(user, request, "31", 2013, 1)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("h2").first().text should be("Remove your company car benefit")
    }

    "Contain correct employee names" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val car = Car(None, None, Some(new LocalDate()), BigDecimal(10), 1, 1, 1, "12000", BigDecimal("1432"))

      val payeRoot = new PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map()) {
        override def employments(taxYear: Int)(implicit payeMicroService: PayeMicroService): Seq[Employment] = { Seq(Employment(1, new LocalDate(), Some(new LocalDate()), "123", "123123", Some("Sainsburys"))) }
        override def benefits(taxYear: Int)(implicit payeMicroService: PayeMicroService): Seq[Benefit] = { Seq(Benefit(31, 2013, BigDecimal("3"), 1, BigDecimal("4"), BigDecimal("5"), BigDecimal("6"), BigDecimal("7"), BigDecimal("8"), "payment", Some(car), Map[String, String](), Map[String, String]())) }
      }

      val user = User("wshakespeare", null, RegimeRoots(Some(payeRoot), None, None), None, None)

      val request: play.api.mvc.Request[_] = FakeRequest().withFormUrlEncodedBody("withdrawDate" -> "2013-07-13", "agreement" -> "true")

      when(controller.payeMicroService.calculateWithdrawBenefit(Matchers.any[Benefit], Matchers.any[LocalDate])).thenReturn(CalculationResult(Map("2013" -> BigDecimal("123"))))

      val result = controller.benefitRemovalFormAction(user, request, "31", 2013, 1)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".checkbox").text should not include "Some("
    }

  }
}
