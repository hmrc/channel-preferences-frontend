package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import play.api.test.{ FakeRequest, WithApplication }
import microservice.MockMicroServicesForTests
import microservice.auth.AuthMicroService
import microservice.paye.{ CalculationResult, PayeMicroService }
import org.mockito.Mockito._
import microservice.paye.domain._
import microservice.auth.domain.{ Regimes, UserAuthority }
import microservice.paye.domain.PayeRoot
import play.api.test.FakeApplication
import microservice.paye.domain.Benefit
import scala.Some
import microservice.paye.domain.TaxCode
import org.joda.time.LocalDate
import views.formatting.Dates
import org.mockito.Matchers
import java.net.URI

class PayeControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption {

  import play.api.test.Helpers._

  private val mockAuthMicroService = mock[AuthMicroService]

  when(mockAuthMicroService.authority("/auth/oid/jdensmore")).thenReturn(
    Some(UserAuthority("/personal/paye/AB123456C", Regimes(paye = Some(URI.create("/personal/paye/AB123456C"))), None)))

  // Configure paye service mock

  private val mockPayeMicroService = mock[PayeMicroService]

  when(mockPayeMicroService.root("/personal/paye/AB123456C")).thenReturn(
    PayeRoot(
      name = "John Densmore",
      nino = "AB123456C",
      version = 22,
      links = Map(
        "taxCode" -> "/personal/paye/AB123456C/tax-codes/2013",
        "employments" -> "/personal/paye/AB123456C/employments/2013",
        "benefits" -> "/personal/paye/AB123456C/benefits/2013")
    )
  )

  when(mockPayeMicroService.linkedResource[Seq[TaxCode]]("/personal/paye/AB123456C/tax-codes/2013")).thenReturn(
    Some(Seq(TaxCode("430L")))
  )

  when(mockPayeMicroService.linkedResource[Seq[Employment]]("/personal/paye/AB123456C/employments/2013")).thenReturn(
    Some(Seq(
      Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = "Weyland-Yutani Corp"),
      Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = "Weyland-Yutani Corp")))
  )

  val carBenefit = Benefit(benefitType = 31, taxYear = 2013, grossAmount = 321.42, employmentSequenceNumber = 2,
    cars = List(Car(None, Some(new LocalDate(2012, 6, 1)), Some(new LocalDate(2012, 12, 12)), 0, 2, 124, 1, "B", BigDecimal("12343.21"))), actions("AB123456C", 2013, 1), Map.empty)

  when(mockPayeMicroService.linkedResource[Seq[Benefit]]("/personal/paye/AB123456C/benefits/2013")).thenReturn(
    Some(Seq(
      Benefit(benefitType = 30, taxYear = 2013, grossAmount = 135.33, employmentSequenceNumber = 1, cars = List(), Map.empty, Map.empty),
      Benefit(benefitType = 29, taxYear = 2013, grossAmount = 22.22, employmentSequenceNumber = 3, cars = List(), actions("AB123456C", 2013, 1), Map.empty),
      carBenefit))
  )

  private def controller = new PayeController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val payeMicroService = mockPayeMicroService
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
      content should include("July 2, 2013 to October 8, 2013")
      content should include("October 14, 2013 to present")
    }

    "return the link to the list of benefits for John Densmore" in new WithApplication(FakeApplication()) {
      val content = requestHome
      content should include("Click here to see your benefits")
    }

    def requestHome: String = {
      val home = controller.home
      val result = home(FakeRequest().withSession(("userId", encrypt("/auth/oid/jdensmore"))))

      status(result) should be(200)

      contentAsString(result)
    }
  }

  "The benefits method" should {

    "display John's benefits" in new WithApplication(FakeApplication()) {
      requestBenefits should include("£ 135.33")
    }

    "not display a benefits without a corresponding employment" in new WithApplication(FakeApplication()) {
      requestBenefits should not include "£ 22.22"
    }

    "display car details" in new WithApplication(FakeApplication()) {
      requestBenefits should include("Medical Insurance")
      requestBenefits should include("Car Benefit")
      requestBenefits should include("Engine size: 0-1400 cc")
      requestBenefits should include("Fuel type: Bi-Fuel")
      requestBenefits should include("Date car registered: December 12, 2012")
      requestBenefits should include("£ 321.42")
    }

    def requestBenefits = {
      val result = controller.listBenefits(FakeRequest().withSession(("userId", encrypt("/auth/oid/jdensmore"))))
      status(result) shouldBe 200
      contentAsString(result)
    }

  }

  "The remove benefit method" should {
    "in step 1 display car details" in new WithApplication(FakeApplication()) {
      val result = controller.removeCarBenefitToStep1(2013, 2)(FakeRequest().withSession(("userId", encrypt("/auth/oid/jdensmore"))))
      status(result) shouldBe 200
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include("Registered on December 12, 2012.")
      requestBenefits should include("Value of car benefit: £ 321.42")
    }

    "in step 1 display an error message when return date of car greater than 35 days" in new WithApplication(FakeApplication()) {
      val invalidWithdrawDate = new LocalDate().plusDays(36)
      val result = controller.removeCarBenefitToStep2(2013, 2)(FakeRequest().withFormUrlEncodedBody("withdraw_date" -> Dates.shortDate(invalidWithdrawDate)).withSession(("userId", encrypt("/auth/oid/jdensmore"))))
      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include("Registered on December 12, 2012.")
      requestBenefits should include("Value of car benefit: £ 321.42")
      requestBenefits should include("Invalid date: Return date cannot be greater than 35 days from today")
    }

    "in step 1 display an error message when return date is not set" in new WithApplication(FakeApplication()) {
      val result = controller.removeCarBenefitToStep2(2013, 2)(FakeRequest().withFormUrlEncodedBody("withdraw_date" -> "").withSession(("userId", encrypt("/auth/oid/jdensmore"))))
      status(result) shouldBe 400
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include("Registered on December 12, 2012.")
      requestBenefits should include("Value of car benefit: £ 321.42")
      requestBenefits should include("Invalid date: Use format DD/MM/YYYY, e.g. 01/12/2013")
    }

    "in step 2 display the calculated value" in new WithApplication(FakeApplication()) {

      val calculationResult = CalculationResult(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(mockPayeMicroService.calculateWithdrawBenefit(Matchers.any[Benefit](), Matchers.any[LocalDate]())).thenReturn(calculationResult)

      val withdrawDate = new LocalDate()
      val result = controller.removeCarBenefitToStep2(2013, 2)(FakeRequest().withFormUrlEncodedBody("withdraw_date" -> Dates.shortDate(withdrawDate)).withSession(("userId", encrypt("/auth/oid/jdensmore"))))
      status(result) shouldBe 200
      val requestBenefits = contentAsString(result)
      requestBenefits should include("Personal Allowance by £ 197.96.")
    }

    "in step 2 save the withdrawDate to the session" in new WithApplication(FakeApplication()) {

      val calculationResult = CalculationResult(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(mockPayeMicroService.calculateWithdrawBenefit(Matchers.any[Benefit](), Matchers.any[LocalDate]())).thenReturn(calculationResult)

      val withdrawDate = new LocalDate()
      val result = controller.removeCarBenefitToStep2(2013, 2)(FakeRequest().withFormUrlEncodedBody("withdraw_date" -> Dates.shortDate(withdrawDate)).withSession(("userId", encrypt("/auth/oid/jdensmore"))))
      session(result).get("withdraw_date") must not be 'empty

    }

    "in step 2 call the paye service to remove the benefit and render the success page" in new WithApplication(FakeApplication()) {

      when(mockPayeMicroService.removeCarBenefit(Matchers.any[String](), Matchers.any[Int](), Matchers.any[Benefit](), Matchers.any[LocalDate]())).thenReturn(Some(Map("message" -> "Done!")))

      val withdrawDate = new LocalDate(2013, 7, 18)
      val result = controller.removeCarBenefitToStep3(2013, 2)(FakeRequest().withSession("userId" -> encrypt("/auth/oid/jdensmore"), "withdraw_date" -> Dates.shortDate(withdrawDate)))

      verify(mockPayeMicroService, times(1)).removeCarBenefit("AB123456C", 22, carBenefit, withdrawDate)

      status(result) shouldBe 303

      headers(result).get("Location") mustBe Some("/paye/benefits/2013/remove/2/3")

    }
  }

}
