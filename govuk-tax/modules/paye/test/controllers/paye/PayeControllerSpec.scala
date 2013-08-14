package controllers.paye

import play.api.test.{ FakeRequest, WithApplication }

import uk.gov.hmrc.microservice.MockMicroServicesForTests
import uk.gov.hmrc.microservice.auth.AuthMicroService
import uk.gov.hmrc.microservice.paye.PayeMicroService
import org.joda.time.{ DateTimeZone, DateTime, LocalDate }
import views.formatting.Dates
import java.net.URI
import controllers.common.SessionTimeoutWrapper._
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

class PayeControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption {

  import play.api.test.Helpers._

  private val mockAuthMicroService = mock[AuthMicroService]
  private val mockPayeMicroService = mock[PayeMicroService]
  private val mockTxQueueMicroService = mock[TxQueueMicroService]

  private val currentTestDate = new DateTime(DateTimeZone.UTC).dayOfMonth().setCopy(13).monthOfYear().setCopy(8).withYear(2013)
  val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

  private def setupUser(id: String, nino: String, name: String) {
    when(mockAuthMicroService.authority(id)).thenReturn(
      Some(UserAuthority(s"/personal/paye/$nino", Regimes(paye = Some(URI.create(s"/personal/paye/$nino"))), None)))

    when(mockPayeMicroService.root(s"/personal/paye/$nino")).thenReturn(
      PayeRoot(
        name = name,
        nino = nino,
        version = 22,
        links = Map(
          "taxCode" -> s"/paye/$nino/tax-codes/2013",
          "employments" -> s"/paye/$nino/employments/2013",
          "benefits" -> s"/paye/$nino/benefits/2013"),
        transactionLinks = Map("accepted" -> s"/txqueue/current-status/paye/$nino/ACCEPTED/after/{from}",
          "completed" -> s"/txqueue/current-status/paye/$nino/COMPLETED/after/{from}",
          "failed" -> s"/txqueue/current-status/paye/$nino/FAILED/after/{from}")
      )
    )
  }

  setupUser("/auth/oid/jdensmore", "AB123456C", "John Densmore")
  setupUser("/auth/oid/removedCar", "RC123456B", "User With Removed Car")

  when(mockPayeMicroService.linkedResource[Seq[TaxCode]]("/paye/AB123456C/tax-codes/2013")).thenReturn(
    Some(Seq(TaxCode(1, 2013, "430L")))
  )

  when(mockPayeMicroService.linkedResource[Seq[Employment]]("/paye/AB123456C/employments/2013")).thenReturn(
    Some(Seq(
      Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = "Weyland-Yutani Corp"),
      Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = "Weyland-Yutani Corp")))
  )

  val carBenefit = Benefit(benefitType = 31, taxYear = 2013, grossAmount = 321.42, employmentSequenceNumber = 2, null, null, null, null, null, null,
    car = Some(Car(None, None, Some(new LocalDate(2012, 12, 12)), 0, 2, 124, 1, "B", BigDecimal("12343.21"))), actions("AB123456C", 2013, 1), Map.empty)

  when(mockPayeMicroService.linkedResource[Seq[Benefit]]("/paye/AB123456C/benefits/2013")).thenReturn(
    Some(Seq(
      Benefit(benefitType = 30, taxYear = 2013, grossAmount = 135.33, employmentSequenceNumber = 1, null, null, null, null, null, null, car = None, Map.empty, Map.empty),
      Benefit(benefitType = 29, taxYear = 2013, grossAmount = 22.22, employmentSequenceNumber = 3, null, null, null, null, null, null, car = None, actions("AB123456C", 2013, 1), Map.empty),
      carBenefit))
  )

  val removedCarBenefit = Benefit(benefitType = 31, taxYear = 2014, grossAmount = 321.42, employmentSequenceNumber = 2, null, null, null, null, null, null,
    car = Some(Car(None, Some(new LocalDate(2013, 7, 12)), Some(new LocalDate(2012, 12, 12)), 0, 2, 124, 1, "B", BigDecimal("12343.21"))), actions("RC123456B", 2013, 1), Map.empty)

  when(mockPayeMicroService.linkedResource[Seq[Benefit]]("/paye/RC123456B/benefits/2013")).thenReturn(
    Some(Seq(
      Benefit(benefitType = 29, taxYear = 2013, grossAmount = 22.22, employmentSequenceNumber = 3, null, null, null, null, null, null, car = None, actions("RC123456B", 2013, 1), Map.empty),
      removedCarBenefit))
  )

  when(mockPayeMicroService.linkedResource[Seq[Employment]]("/paye/RC123456B/employments/2013")).thenReturn(
    Some(Seq(
      Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = "Weyland-Yutani Corp"),
      Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = "Weyland-Yutani Corp")))
  )

  def transactionWithTags(tags: List[String]) = TxQueueTransaction(URI.create("http://tax.com"), "paye", URI.create("http://tax.com"), 1, 2013, None, List(Status("created", None, currentTestDate.minusDays(5))), Some(tags), currentTestDate, currentTestDate.minusDays(1))

  val testTransaction1 = transactionWithTags(List("paye", "test", "message.code.removeCarBenefits"))
  val testTransaction2 = transactionWithTags(List("paye", "test"))

  when(mockTxQueueMicroService.transaction("/txqueue/current-status/paye/AB123456C/ACCEPTED/after/" + dateFormat.print(currentTestDate.minusMonths(1)))).thenReturn(Some(List(testTransaction1, testTransaction2)))
  when(mockTxQueueMicroService.transaction("/txqueue/current-status/paye/AB123456C/COMPLETED/after/" + dateFormat.print(currentTestDate.minusMonths(1)))).thenReturn(Some(List(testTransaction1, testTransaction2)))

  private def controller = new PayeController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val payeMicroService = mockPayeMicroService
    override val txQueueMicroService = mockTxQueueMicroService
    override def currentDate = currentTestDate
  }

  private def actions(nino: String, year: Int, esn: Int): Map[String, String] = {
    Map("updateCar" -> s"/paye/$nino/benefits/$year/$esn/update/car")
  }

  "The home method" should {

    "display the name for John Densmore" in new WithApplication(FakeApplication()) {
      val content = requestHome
      content should include("John Densmore")
    }

    "display the tax codes for John Densmore" in new WithApplication(FakeApplication()) {
      val content = requestHome
      content should include("430L")
    }

    "display the employments for John Densmore" in new WithApplication(FakeApplication()) {
      val content = requestHome
      content should include("898")
      content should include("9900112")
      content should include("899")
      content should include("1212121")
      content should include("Weyland-Yutani Corp")
      content should include("July 2, 2013 to October 8, 2013")
      content should include("October 14, 2013 to present")
    }

    "display recent transactions for John Densmore" in new WithApplication(FakeApplication()) {
      val content = requestHome
      content should include("On August 8, 2013, you removed your company car benefit from Weyland-Yutani Corp. This is being processed and you will receive a new Tax Code within 2 days.")
      content should include("On August 8, 2013, you removed your company car benefit from Weyland-Yutani Corp. This has been processed and your new Tax Code is 430L. Weyland-Yutani Corp have been notified.")
    }

    "return the link to the list of benefits for John Densmore" in new WithApplication(FakeApplication()) {
      val content = requestHome
      content should include("Click here to see your benefits")
    }

    def requestHome: String = {
      val home = controller.home
      val result = home(FakeRequest().withSession("userId" -> encrypt("/auth/oid/jdensmore"), sessionTimestampKey -> controller.now().getMillis.toString))

      status(result) should be(200)

      contentAsString(result)
    }
  }

  "The benefits list page" should {

    "display John's benefits" in new WithApplication(FakeApplication()) {
      requestBenefits("/auth/oid/jdensmore") should include("£ 135.33")
    }

    "not display a benefits without a corresponding employment" in new WithApplication(FakeApplication()) {
      requestBenefits("/auth/oid/jdensmore") should not include "£ 22.22"
    }

    "display car details" in new WithApplication(FakeApplication()) {
      requestBenefits("/auth/oid/jdensmore") should include("Medical Insurance")
      requestBenefits("/auth/oid/jdensmore") should include("Car Benefit")
      requestBenefits("/auth/oid/jdensmore") should include("Weyland-Yutani Corp")
      requestBenefits("/auth/oid/jdensmore") should include("Engine size: 0-1400 cc")
      requestBenefits("/auth/oid/jdensmore") should include("Fuel type: Bi-Fuel")
      requestBenefits("/auth/oid/jdensmore") should include("Date car registered: December 12, 2012")
      requestBenefits("/auth/oid/jdensmore") should include("£ 321.42")
    }
    "display a remove link for car benefits" in new WithApplication(FakeApplication()) {
      requestBenefits("/auth/oid/jdensmore") should include("""href="/benefits/2013/remove/2/1"""")
    }

    "display a Car removed if the withdrawn date is set" in new WithApplication(FakeApplication()) {
      requestBenefits("/auth/oid/removedCar") should include regex "Car removed on.+July 12, 2013".r
    }

    def requestBenefits(id: String) = {
      val result = controller.listBenefits(FakeRequest().withSession("userId" -> encrypt(id), sessionTimestampKey -> controller.now().getMillis.toString))
      status(result) shouldBe 200
      contentAsString(result)
    }

  }

  "The remove benefit method" should {
    "in step 1 display car details" in new WithApplication(FakeApplication()) {
      val result = controller.removeCarBenefitToStep1(2013, 2)(FakeRequest().withSession("userId" -> encrypt("/auth/oid/jdensmore"), sessionTimestampKey -> controller.now().getMillis.toString))
      status(result) shouldBe 200
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£ 321.42".r
    }

    "in step 1 display an error message when return date of car greater than 7 days" in new WithApplication(FakeApplication()) {
      val invalidWithdrawDate = new LocalDate().plusDays(36)
      val result = controller.removeCarBenefitToStep2(2013, 2)(FakeRequest().withFormUrlEncodedBody("withdrawDate" -> Dates.shortDate(invalidWithdrawDate), "agreement" -> "true")
        .withSession("userId" -> encrypt("/auth/oid/jdensmore"), sessionTimestampKey -> controller.now().getMillis.toString))
      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£ 321.42".r
      requestBenefits should include("Invalid date: Return date cannot be greater than 7 days from today")
    }

    "in step 1 display an error message when return date of the car is in the previous tax year" in new WithApplication(FakeApplication()) {
      val invalidWithdrawDate = new LocalDate(1999, 2, 1)
      val result = controller.removeCarBenefitToStep2(2013, 2)(FakeRequest().withFormUrlEncodedBody("withdrawDate" -> Dates.shortDate(invalidWithdrawDate), "agreement" -> "true")
        .withSession("userId" -> encrypt("/auth/oid/jdensmore"), sessionTimestampKey -> controller.now().getMillis.toString))
      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£ 321.42".r
      requestBenefits should include("Invalid date: Return date cannot be in previous tax years")
    }

    "in step 1 display an error message when return date of the car is in the next tax year" in new WithApplication(FakeApplication()) {
      val invalidWithdrawDate = new LocalDate(2030, 2, 1)
      val result = controller.removeCarBenefitToStep2(2013, 2)(FakeRequest().withFormUrlEncodedBody("withdrawDate" -> Dates.shortDate(invalidWithdrawDate), "agreement" -> "true")
        .withSession("userId" -> encrypt("/auth/oid/jdensmore"), sessionTimestampKey -> controller.now().getMillis.toString))
      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£ 321.42".r
      requestBenefits should include("Invalid date: Return date cannot be in next tax years")
    }

    "in step 1 display an error message when return date is not set" in new WithApplication(FakeApplication()) {
      val result = controller.removeCarBenefitToStep2(2013, 2)(FakeRequest().withFormUrlEncodedBody("withdrawDate" -> "", "agreement" -> "true")
        .withSession("userId" -> encrypt("/auth/oid/jdensmore"), sessionTimestampKey -> controller.now().getMillis.toString))
      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£ 321.42".r
      requestBenefits should include("Invalid date: Use format DD/MM/YYYY, e.g. 01/12/2013")
    }

    "in step 1 display an error message when agreement checkbox is not selected" in new WithApplication(FakeApplication()) {
      val result = controller.removeCarBenefitToStep2(2013, 2)(FakeRequest().withFormUrlEncodedBody("withdrawDate" -> "")
        .withSession("userId" -> encrypt("/auth/oid/jdensmore"), sessionTimestampKey -> controller.now().getMillis.toString))
      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include regex "Registered on.*December 12, 2012.".r
      requestBenefits should include regex "Value of car benefit:.*£ 321.42".r
      requestBenefits should include("Invalid date: Use format DD/MM/YYYY, e.g. 01/12/2013")
    }

    "in step 2 display the calculated value" in new WithApplication(FakeApplication()) {

      val calculationResult = CalculationResult(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(mockPayeMicroService.calculateWithdrawBenefit(Matchers.any[Benefit](), Matchers.any[LocalDate]())).thenReturn(calculationResult)

      val withdrawDate = new LocalDate()
      val result = controller.removeCarBenefitToStep2(2013, 2)(FakeRequest().withFormUrlEncodedBody("withdrawDate" -> Dates.shortDate(withdrawDate), "agreement" -> "true")
        .withSession("userId" -> encrypt("/auth/oid/jdensmore"), sessionTimestampKey -> controller.now().getMillis.toString))
      status(result) shouldBe 200
      val requestBenefits = contentAsString(result)
      requestBenefits should include regex "Personal Allowance by.*£ 197.96.".r
    }

    "in step 2 save the withdrawDate to the session" in new WithApplication(FakeApplication()) {
      val revisedAmount = BigDecimal(123.46)
      val withdrawDate = new LocalDate()

      val calculationResult = CalculationResult(Map("2013" -> revisedAmount, "2014" -> BigDecimal(0)))
      when(mockPayeMicroService.calculateWithdrawBenefit(Matchers.any[Benefit](), Matchers.any[LocalDate]())).thenReturn(calculationResult)

      val result = controller.removeCarBenefitToStep2(2013, 2)(FakeRequest().withFormUrlEncodedBody("withdrawDate" -> Dates.shortDate(withdrawDate), "agreement" -> "true")
        .withSession("userId" -> encrypt("/auth/oid/jdensmore"), sessionTimestampKey -> controller.now().getMillis.toString))

      session(result).data must contain key "withdraw_date"
      session(result).data must contain key "revised_amount"
      Dates.parseShortDate(session(result)("withdraw_date")) mustBe withdrawDate
      BigDecimal(session(result)("revised_amount")) mustBe revisedAmount

    }

    "in step 2 call the paye service to remove the benefit and render the success page" in new WithApplication(FakeApplication()) {

      when(mockPayeMicroService.removeCarBenefit(Matchers.any[String](), Matchers.any[Int](), Matchers.any[Benefit](), Matchers.any[LocalDate](), Matchers.any[BigDecimal]())).thenReturn(Some(TransactionId("someId")))

      val withdrawDate = new LocalDate(2013, 7, 18)
      val result = controller.removeCarBenefitToStep3(2013, 2)(FakeRequest()
        .withSession("userId" -> encrypt("/auth/oid/jdensmore"), "withdraw_date" -> Dates.shortDate(withdrawDate), "revised_amount" -> "123.45", sessionTimestampKey -> controller.now().getMillis.toString))

      verify(mockPayeMicroService, times(1)).removeCarBenefit("AB123456C", 22, carBenefit, withdrawDate, BigDecimal("123.45"))

      status(result) shouldBe 303

      headers(result).get("Location") mustBe Some("/benefits/2013/remove/2/3/someId")

    }
  }

}
