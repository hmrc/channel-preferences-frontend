package controllers.paye

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import controllers.common.CookieEncryption
import org.scalatest.{ BeforeAndAfterEachTestData, TestData, BeforeAndAfterEach }
import org.joda.time.{ DateTimeUtils, LocalDate }
import play.api.test.{ WithApplication, FakeRequest }
import views.formatting.Dates
import play.api.test.Helpers._
import uk.gov.hmrc.microservice.paye.PayeMicroService
import org.mockito.Mockito._
import org.mockito.Matchers
import uk.gov.hmrc.microservice.paye.domain._
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.microservice.paye.CalculationResult
import scala.Some
import uk.gov.hmrc.microservice.paye.domain.Employment
import play.api.test.FakeApplication
import uk.gov.hmrc.microservice.paye.domain.Benefit
import uk.gov.hmrc.microservice.paye.domain.TaxCode
import uk.gov.hmrc.microservice.domain.{ RegimeRoots, User }
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.keystore.KeyStore

class RemoveBenefitControllerSpec extends PayeBaseSpec with MockitoSugar with CookieEncryption {

  private lazy val controller = new RemoveBenefitController with MockMicroServicesForTests

  override protected def beforeEach(testData: TestData) {
    super.beforeEach(testData)

    controller.resetAll()
  }

  private def setupMocksForJohnDensmore(taxCodes: Seq[TaxCode], employments: Seq[Employment], benefits: Seq[Benefit],
    acceptedTransactions: List[TxQueueTransaction], completedTransactions: List[TxQueueTransaction]) {
    when(controller.payeMicroService.linkedResource[Seq[TaxCode]]("/paye/AB123456C/tax-codes/2013")).thenReturn(Some(taxCodes))
    when(controller.payeMicroService.linkedResource[Seq[Employment]]("/paye/AB123456C/employments/2013")).thenReturn(Some(employments))
    when(controller.payeMicroService.linkedResource[Seq[Benefit]]("/paye/AB123456C/benefits/2013")).thenReturn(Some(benefits))
    when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/ACCEPTED/.*"))).thenReturn(Some(acceptedTransactions))
    when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/COMPLETED/.*"))).thenReturn(Some(completedTransactions))
  }

  "The car benefit removal form" should {
    "give the option to remove the fuel benefit if the user has one" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val result = controller.benefitRemovalFormAction(31, johnDensmore, FakeRequest(), 2013, 2)
      status(result) shouldBe 200
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include("remove my fuel benefit")

    }

    "not give the option to remove a fuel benefit if the user does not have one" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefit), List.empty, List.empty)

      val result = controller.benefitRemovalFormAction(31, johnDensmore, FakeRequest(), 2013, 2)
      status(result) shouldBe 200
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should not include ("remove my fuel benefit")

    }
  }

  "The remove benefit method" should {

    def requestBenefitRemovalFormSubmission(date: Option[LocalDate], agreed: Boolean) =
      FakeRequest().withFormUrlEncodedBody("withdrawDate" -> date.map(Dates.shortDate(_)).getOrElse(""), "agreement" -> agreed.toString.toLowerCase)

    "in step 1 display car details" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val result = controller.benefitRemovalFormAction(31, johnDensmore, FakeRequest(), 2013, 2)
      status(result) shouldBe 200
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£ 321.42".r
    }

    "in step 1 display an error message when return date of car greater than 7 days" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val invalidWithdrawDate = new LocalDate().plusDays(36)
      val result = controller.requestBenefitRemovalAction(31, johnDensmore,
        requestBenefitRemovalFormSubmission(Some(invalidWithdrawDate), true), 2013, 2)

      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£ 321.42".r
      requestBenefits should include("Invalid date: Return date cannot be greater than 7 days from today")
    }

    "in step 1 display an error message when return date of the car is in the previous tax year" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val invalidWithdrawDate = new LocalDate(1999, 2, 1)
      val result = controller.requestBenefitRemovalAction(31, johnDensmore,
        requestBenefitRemovalFormSubmission(Some(invalidWithdrawDate), true), 2013, 2)

      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£ 321.42".r
      requestBenefits should include("Invalid date: Return date cannot be in previous tax years")
    }

    "in step 1 display an error message when return date of the car is in the next tax year" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val invalidWithdrawDate = new LocalDate(2030, 2, 1)
      val result = controller.requestBenefitRemovalAction(31, johnDensmore,
        requestBenefitRemovalFormSubmission(Some(invalidWithdrawDate), true), 2013, 2)

      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£ 321.42".r
      requestBenefits should include("Invalid date: Return date cannot be in next tax years")
    }

    "in step 1 display an error message when return date is not set" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val result = controller.requestBenefitRemovalAction(31, johnDensmore, requestBenefitRemovalFormSubmission(None, true), 2013, 2)

      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£ 321.42".r
      requestBenefits should include("Invalid date: Use format DD/MM/YYYY, e.g. 01/12/2013")
    }

    "in step 1 display an error message when agreement checkbox is not selected" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val result = controller.requestBenefitRemovalAction(31, johnDensmore, FakeRequest().withFormUrlEncodedBody("withdrawDate" -> ""), 2013, 2)

      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£ 321.42".r
      requestBenefits should include("Invalid date: Use format DD/MM/YYYY, e.g. 01/12/2013")
    }

    "in step 2 display the calculated value" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val calculationResult = CalculationResult(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(controller.payeMicroService.calculateWithdrawBenefit(Matchers.any[Benefit](), Matchers.any[LocalDate]())).thenReturn(calculationResult)

      val withdrawDate = new LocalDate()
      val result = controller.requestBenefitRemovalAction(31, johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true), 2013, 2)

      status(result) shouldBe 200
      val requestBenefits = contentAsString(result)
      requestBenefits should include regex "Personal Allowance by.*£ 197.96.".r
    }

    "in step 2 save the withdrawDate to the keystore" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val revisedAmount = BigDecimal(123.46)
      val withdrawDate = new LocalDate()

      val calculationResult = CalculationResult(Map("2013" -> revisedAmount, "2014" -> BigDecimal(0)))
      when(controller.payeMicroService.calculateWithdrawBenefit(Matchers.any[Benefit](), Matchers.any[LocalDate]())).thenReturn(calculationResult)

      val result = controller.requestBenefitRemovalAction(31, johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true), 2013, 2)

      verify(controller.keyStoreMicroService, times(1)).addKeyStoreEntry(johnDensmore.oid, "paye_ui", "remove_benefit", Map("form" -> RemoveBenefitData(withdrawDate, "123.46")))
    }

    "in step 2 call the paye service to remove the benefit and render the success page" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      when(controller.payeMicroService.removeBenefit(Matchers.any[String], Matchers.any[String](), Matchers.any[Int](), Matchers.any[Benefit](), Matchers.any[LocalDate](), Matchers.any[BigDecimal]())).thenReturn(Some(TransactionId("someId")))

      val withdrawDate = new LocalDate(2013, 7, 18)
      when(controller.keyStoreMicroService.getEntry[RemoveBenefitData](johnDensmore.oid, "paye_ui", "remove_benefit", "form")).thenReturn(Some(RemoveBenefitData(withdrawDate, "123.45")))

      val result = controller.confirmBenefitRemovalAction(31, johnDensmore, FakeRequest(), 2013, 2)

      verify(controller.payeMicroService, times(1)).removeBenefit("/paye/AB123456C/benefits/2013/1/update/cars", "AB123456C", 22, carBenefit, withdrawDate, BigDecimal("123.45"))

      status(result) shouldBe 303

      headers(result).get("Location") shouldBe Some("/benefits/31/confirmation/someId")

    }

    "in step 3 show the transaction id only if the transaction exists" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val transaction: TxQueueTransaction = mock[TxQueueTransaction]
      when(controller.txQueueMicroService.transaction(Matchers.eq("123"), Matchers.any[PayeRoot])).thenReturn(Some(transaction))

      val withdrawDate = new LocalDate(2013, 7, 18)
      when(controller.keyStoreMicroService.getEntry[RemoveBenefitData](johnDensmore.oid, "paye_ui", "remove_benefit", "form")).thenReturn(Some(RemoveBenefitData(withdrawDate, "123.45")))

      val result = controller.benefitRemovedAction(johnDensmore, FakeRequest(), 31, "123")

      status(result) shouldBe 200
      contentAsString(result) should include("123")

    }

    "in step 3 return 404 if the transaction does not exist" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      when(controller.txQueueMicroService.transaction(Matchers.eq("123"), Matchers.any[PayeRoot])).thenReturn(None)
      when(controller.keyStoreMicroService.getEntry[RemoveBenefitData](johnDensmore.oid, "paye_ui", "remove_benefit", "form")).thenReturn(Some(RemoveBenefitData(withdrawDate, "555")))

      val withdrawDate = new LocalDate(2013, 7, 18)
      val result = controller.benefitRemovedAction(johnDensmore, FakeRequest().withSession("withdraw_date" -> Dates.shortDate(withdrawDate)), 31, "123")

      status(result) shouldBe 404

    }

    "return the updated benefits list page if the user has gone back in the browser and resubmitted and the benefit has already been removed" in {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List(removedCarTransaction), List.empty)

      val result = controller.benefitRemovalFormAction(31, johnDensmore, FakeRequest(), 2013, 1)
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/benefits")
    }

    "return the benefits list page if the user modifies the url to include a benefit type that they can not remove" in {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List(removedCarTransaction), List.empty)

      val result = controller.benefitRemovalFormAction(30, johnDensmore, FakeRequest(), 2013, 1)
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/benefits")
    }

    "return to the benefits list page if the user modifies the url to include an incorrect sequence number" in {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List(removedCarTransaction), List.empty)

      val result = controller.benefitRemovalFormAction(31, johnDensmore, FakeRequest(), 2013, 3)
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

      val result = controller.requestBenefitRemovalAction(31, user, request, 2013, 1)
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

      val result = controller.benefitRemovalFormAction(31, user, request, 2013, 1)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".checkbox").text should not include "Some("
    }

  }
}
