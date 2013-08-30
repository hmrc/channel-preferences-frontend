package controllers.paye

import play.api.test.{ FakeRequest, WithApplication }

import uk.gov.hmrc.microservice.MockMicroServicesForTests
import uk.gov.hmrc.microservice.paye.PayeMicroService
import org.joda.time.{ DateTime, LocalDate }
import views.formatting.Dates
import java.net.URI
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers
import uk.gov.hmrc.common.BaseSpec
import controllers.common.CookieEncryption
import uk.gov.hmrc.microservice.txqueue._
import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.microservice.paye.CalculationResult
import scala.Some
import uk.gov.hmrc.microservice.paye.domain.Employment
import uk.gov.hmrc.microservice.auth.domain.Regimes
import uk.gov.hmrc.microservice.paye.domain.Car
import play.api.test.FakeApplication
import uk.gov.hmrc.microservice.paye.domain.Benefit
import uk.gov.hmrc.microservice.paye.domain.TaxCode
import uk.gov.hmrc.microservice.paye.domain.TransactionId
import uk.gov.hmrc.microservice.domain.{ RegimeRoots, User }
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach

class PayeControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption with BeforeAndAfterEach {

  import play.api.test.Helpers._

  private lazy val controller = new PayeController with MockMicroServicesForTests

  private val currentTestDate = new DateTime()
  val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

  private def setupUser(id: String, nino: String, name: String): User = {
    val ua = UserAuthority(s"/personal/paye/$nino", Regimes(paye = Some(URI.create(s"/personal/paye/$nino"))), None)

    val payeRoot = PayeRoot(
      name = name,
      firstName = "Barney",
      secondName = None,
      surname = "Rubble",
      nino = nino,
      version = 22,
      title = "Mr",
      dateOfBirth = "1976-04-12",
      links = Map(
        "taxCode" -> s"/paye/$nino/tax-codes/2013",
        "employments" -> s"/paye/$nino/employments/2013",
        "benefits" -> s"/paye/$nino/benefits/2013"),
      transactionLinks = Map("accepted" -> s"/txqueue/current-status/paye/$nino/ACCEPTED/after/{from}",
        "completed" -> s"/txqueue/current-status/paye/$nino/COMPLETED/after/{from}",
        "failed" -> s"/txqueue/current-status/paye/$nino/FAILED/after/{from}",
        "findByOid" -> "/txqueue/oid/{oid}")
    )

    User(id, ua, RegimeRoots(Some(payeRoot), None, None), None, None)
  }

  val johnDensmore = setupUser("/auth/oid/jdensmore", "AB123456C", "John Densmore")

  val userWithRemovedCar = setupUser("/auth/oid/removedCar", "RC123456B", "User With Removed Car")

  val johnDensmoresTaxCodes = Seq(TaxCode(1, 2013, "430L"))

  val johnDensmoresEmployments = Seq(
    Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Weyland-Yutani Corp")),
    Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None))

  val carBenefit = Benefit(benefitType = 31, taxYear = 2013, grossAmount = 321.42, employmentSequenceNumber = 2, null, null, null, null, null, null,
    car = Some(Car(None, None, Some(new LocalDate(2012, 12, 12)), 0, 2, 124, 1, "B", BigDecimal("12343.21"))), actions("AB123456C", 2013, 1), Map.empty)

  val johnDensmoresBenefits = Seq(
    Benefit(benefitType = 30, taxYear = 2013, grossAmount = 135.33, employmentSequenceNumber = 1, null, null, null, null, null, null, car = None, Map.empty, Map.empty),
    Benefit(benefitType = 29, taxYear = 2013, grossAmount = 22.22, employmentSequenceNumber = 3, null, null, null, null, null, null, car = None, actions("AB123456C", 2013, 1), Map.empty),
    carBenefit)

  val removedCarBenefit = Benefit(benefitType = 31, taxYear = 2013, grossAmount = 321.42, employmentSequenceNumber = 1, null, null, null, null, null, null,
    car = Some(Car(None, Some(new LocalDate(2013, 7, 12)), Some(new LocalDate(2012, 12, 12)), 0, 2, 124, 1, "B", BigDecimal("12343.21"))), actions("RC123456B", 2013, 1), Map.empty)

  val userWithRemovedCarEmployments = Some(Seq(
    Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Sansbury")),
    Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None)))

  val userWithRemovedCarBenefits = Some(Seq(
    Benefit(benefitType = 29, taxYear = 2013, grossAmount = 22.22, employmentSequenceNumber = 3, null, null, null, null, null, null, car = None, actions("RC123456B", 2013, 1), Map.empty),
    removedCarBenefit))

  def transactionWithTags(tags: List[String], properties: Map[String, String] = Map.empty) =
    TxQueueTransaction(URI.create("http://tax.com"),
      "paye",
      URI.create("http://tax.com"),
      None,
      List(Status("created", None, currentTestDate)),
      Some(tags),
      properties ++ Map("employmentSequenceNumber" -> "1", "taxYear" -> "2013"),
      currentTestDate,
      currentTestDate.minusDays(1))

  val removedCarTransaction = transactionWithTags(List("paye", "test", "message.code.removeCarBenefits"), Map("benefitType" -> "31"))
  val otherTransaction = transactionWithTags(List("paye", "test"))
  val removedFuelTransaction = transactionWithTags(List("paye", "test", "message.code.removeFuelBenefits"), Map("benefitType" -> "29"))

  val testTransactions = List(removedCarTransaction, otherTransaction, removedFuelTransaction)

  val completedTransactions = List(otherTransaction, removedFuelTransaction)

  val acceptedTransactions = List(removedCarTransaction)

  private def actions(nino: String, year: Int, esn: Int): Map[String, String] = {
    Map("removeCar" -> s"/paye/$nino/benefits/$year/$esn/update/cars")
  }

  private def setupMocksForJohnDensmore(taxCodes: Seq[TaxCode], employments: Seq[Employment], benefits: Seq[Benefit],
    acceptedTransactions: List[TxQueueTransaction], completedTransactions: List[TxQueueTransaction]) {
    when(controller.payeMicroService.linkedResource[Seq[TaxCode]]("/paye/AB123456C/tax-codes/2013")).thenReturn(Some(taxCodes))
    when(controller.payeMicroService.linkedResource[Seq[Employment]]("/paye/AB123456C/employments/2013")).thenReturn(Some(employments))
    when(controller.payeMicroService.linkedResource[Seq[Benefit]]("/paye/AB123456C/benefits/2013")).thenReturn(Some(benefits))
    when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/ACCEPTED/.*"))).thenReturn(Some(acceptedTransactions))
    when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/COMPLETED/.*"))).thenReturn(Some(completedTransactions))
  }

  "The home method" should {

    "display the name for John Densmore" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val content = requestHomeAction
      content should include("John Densmore")
    }

    "display the tax codes for John Densmore" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val content = requestHomeAction
      content should include("430L")
    }

    "display the employments for John Densmore" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val content = requestHomeAction
      content should include("Weyland-Yutani Corp")
      content should include("899")
      content should include("1212121")
      content should include("July 2, 2013 to October 8, 2013")
      content should include("October 14, 2013 to present")
    }

    "display employer ref when the employer name is missing" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val content = requestHomeAction
      content should include("1212121")
    }

    "display recent transactions for John Densmore" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, testTransactions, testTransactions)

      val content = requestHomeAction
      content should include(s"On ${Dates.formatDate(currentTestDate.toLocalDate)}, you removed your company car benefit from Weyland-Yutani Corp. This is being processed and you will receive a new Tax Code within 2 days.")
      content should include(s"On ${Dates.formatDate(currentTestDate.toLocalDate)}, you removed your company car benefit from Weyland-Yutani Corp. This has been processed and your new Tax Code is 430L. Weyland-Yutani Corp have been notified.")

      content should include(s"On ${Dates.formatDate(currentTestDate.toLocalDate)}, you removed your company car fuel benefit from Weyland-Yutani Corp. This is being processed and you will receive a new Tax Code within 2 days.")
      content should include(s"On ${Dates.formatDate(currentTestDate.toLocalDate)}, you removed your company car fuel benefit from Weyland-Yutani Corp. This has been processed and your new Tax Code is 430L. Weyland-Yutani Corp have been notified.")
    }

    "return the link to the list of benefits for John Densmore" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val content = requestHomeAction
      content should include("Click here to see your benefits")
    }

    def requestHomeAction: String = {
      val homeAction = controller.homeAction
      val result = homeAction(johnDensmore, FakeRequest())
      status(result) should be(200)
      contentAsString(result)
    }
  }

  "The benefits list page" should {

    "display John's benefits" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      requestBenefitsAction(johnDensmore) should include("£ 135.33")
    }

    "not display a benefits without a corresponding employment" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      requestBenefitsAction(johnDensmore) should not include "£ 22.22"
    }

    "display car details" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      requestBenefitsAction(johnDensmore) should include("Medical Insurance")
      requestBenefitsAction(johnDensmore) should include("Car Benefit")
      requestBenefitsAction(johnDensmore) should include("899/1212121")
      requestBenefitsAction(johnDensmore) should include("Engine size: 0-1400 cc")
      requestBenefitsAction(johnDensmore) should include("Fuel type: Bi-Fuel")
      requestBenefitsAction(johnDensmore) should include("Date car registered: December 12, 2012")
      requestBenefitsAction(johnDensmore) should include("£ 321.42")
    }

    "display a remove link for car benefits" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      requestBenefitsAction(johnDensmore) should include("""href="/benefits/31/2013/2/remove"""")
    }

    "display a Car removed if there is an accepted transaction present for the car benefit" in new WithApplication(FakeApplication()) {
      controller.resetAll
      when(controller.payeMicroService.linkedResource[Seq[Employment]]("/paye/RC123456B/employments/2013")).thenReturn(userWithRemovedCarEmployments)
      when(controller.payeMicroService.linkedResource[Seq[Benefit]]("/paye/RC123456B/benefits/2013")).thenReturn(userWithRemovedCarBenefits)
      when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/RC123456B/ACCEPTED/.*"))).thenReturn(Some(acceptedTransactions))
      when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/RC123456B/COMPLETED/.*"))).thenReturn(Some(List.empty))

      val result = requestBenefitsAction(userWithRemovedCar)
      println(result)
      result should include("Benefit removed")
    }

    "display a benefit removed if there is a completed transaction present for the car benefit" in new WithApplication(FakeApplication()) {
      controller.resetAll
      when(controller.payeMicroService.linkedResource[Seq[Employment]]("/paye/RC123456B/employments/2013")).thenReturn(userWithRemovedCarEmployments)
      when(controller.payeMicroService.linkedResource[Seq[Benefit]]("/paye/RC123456B/benefits/2013")).thenReturn(userWithRemovedCarBenefits)
      when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/RC123456B/ACCEPTED/.*"))).thenReturn(Some(List.empty))
      when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/RC123456B/COMPLETED/.*"))).thenReturn(Some(List(removedCarTransaction)))

      val result = requestBenefitsAction(userWithRemovedCar)
      println(result)
      result should include("Benefit removed")
    }

    def requestBenefitsAction(user: User) = {
      val result = controller.listBenefitsAction(user, FakeRequest())
      status(result) shouldBe 200
      contentAsString(result)
    }

  }

  "The remove benefit method" should {

    def requestBenefitRemovalFormSubmission(date: Option[LocalDate], agreed: Boolean) =
      FakeRequest().withFormUrlEncodedBody("withdrawDate" -> date.map(Dates.shortDate(_)).getOrElse(""), "agreement" -> agreed.toString.toLowerCase)

    "in step 1 display car details" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val result = controller.benefitRemovalFormAction(31, johnDensmore, FakeRequest(), 2013, 2)
      status(result) shouldBe 200
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£ 321.42".r
    }

    "in step 1 display an error message when return date of car greater than 7 days" in new WithApplication(FakeApplication()) {
      controller.resetAll
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
      controller.resetAll
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
      controller.resetAll
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
      controller.resetAll
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
      controller.resetAll
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
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val calculationResult = CalculationResult(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(controller.payeMicroService.calculateWithdrawBenefit(Matchers.any[Benefit](), Matchers.any[LocalDate]())).thenReturn(calculationResult)

      val withdrawDate = new LocalDate()
      val result = controller.requestBenefitRemovalAction(31, johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true), 2013, 2)

      status(result) shouldBe 200
      val requestBenefits = contentAsString(result)
      requestBenefits should include regex "Personal Allowance by.*£ 197.96.".r
    }

    "in step 2 save the withdrawDate to the session" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val revisedAmount = BigDecimal(123.46)
      val withdrawDate = new LocalDate()

      val calculationResult = CalculationResult(Map("2013" -> revisedAmount, "2014" -> BigDecimal(0)))
      when(controller.payeMicroService.calculateWithdrawBenefit(Matchers.any[Benefit](), Matchers.any[LocalDate]())).thenReturn(calculationResult)

      val result = controller.requestBenefitRemovalAction(31, johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true), 2013, 2)

      session(result).data must contain key "withdraw_date"
      session(result).data must contain key "revised_amount"
      Dates.parseShortDate(session(result)("withdraw_date")) mustBe withdrawDate
      BigDecimal(session(result)("revised_amount")) mustBe revisedAmount

    }

    "in step 2 call the paye service to remove the benefit and render the success page" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      when(controller.payeMicroService.removeBenefit(Matchers.any[String], Matchers.any[String](), Matchers.any[Int](), Matchers.any[Benefit](), Matchers.any[LocalDate](), Matchers.any[BigDecimal]())).thenReturn(Some(TransactionId("someId")))

      val withdrawDate = new LocalDate(2013, 7, 18)
      val result = controller.confirmBenefitRemovalAction(31, johnDensmore,
        FakeRequest().withSession("withdraw_date" -> Dates.shortDate(withdrawDate), "revised_amount" -> "123.45"), 2013, 2)

      verify(controller.payeMicroService, times(1)).removeBenefit("/paye/AB123456C/benefits/2013/1/update/cars", "AB123456C", 22, carBenefit, withdrawDate, BigDecimal("123.45"))

      status(result) shouldBe 303

      headers(result).get("Location") mustBe Some("/benefits/31/confirmation/someId")

    }

    "in step 3 show the transaction id only if the transaction exists" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      val transaction: TxQueueTransaction = mock[TxQueueTransaction]
      when(controller.txQueueMicroService.transaction(Matchers.eq("123"), Matchers.any[PayeRoot])).thenReturn(Some(transaction))

      val withdrawDate = new LocalDate(2013, 7, 18)
      val result = controller.benefitRemovedAction(johnDensmore, FakeRequest().withSession("withdraw_date" -> Dates.shortDate(withdrawDate)), 31, "123")

      status(result) shouldBe 200
      contentAsString(result) must include("123")

    }

    "in step 3 return 404 if the transaction does not exist" in new WithApplication(FakeApplication()) {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List.empty, List.empty)

      when(controller.txQueueMicroService.transaction(Matchers.eq("123"), Matchers.any[PayeRoot])).thenReturn(None)

      val withdrawDate = new LocalDate(2013, 7, 18)
      val result = controller.benefitRemovedAction(johnDensmore, FakeRequest().withSession("withdraw_date" -> Dates.shortDate(withdrawDate)), 31, "123")

      status(result) shouldBe 404

    }

    "return the updated benefits list page if the user has gone back in the browser and resubmitted and the benefit has already been removed" in {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List(removedCarTransaction), List.empty)

      val result = controller.benefitRemovalFormAction(31, johnDensmore, FakeRequest(), 2013, 1)
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/benefits")
    }

    "return the benefits list page if the user modifies the url to include a benefit type that they can not remove" in {
      controller.resetAll
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits, List(removedCarTransaction), List.empty)

      val result = controller.benefitRemovalFormAction(30, johnDensmore, FakeRequest(), 2013, 1)
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/benefits")
    }
  }

  "benefitRemoved" should {
    "render a view with correct elements" in new WithApplication(FakeApplication()) {
      controller.resetAll
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
      controller.resetAll
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
      doc.select(".checkbox").text should not include ("Some(")
    }

  }

}
