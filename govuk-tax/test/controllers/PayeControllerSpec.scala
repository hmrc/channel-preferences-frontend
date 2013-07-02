package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import play.api.test.{ FakeRequest, WithApplication }
import microservices.MockMicroServicesForTests
import microservice.auth.AuthMicroService
import microservice.paye.PayeMicroService
import org.mockito.Mockito._
import microservice.paye.domain._
import microservice.auth.domain.UserAuthority
import microservice.paye.domain.PayeRoot
import play.api.test.FakeApplication
import microservice.paye.domain.Benefit
import scala.Some
import microservice.paye.domain.TaxCode
import play.api.mvc.Cookie
import org.joda.time.LocalDate

class PayeControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption {

  import play.api.test.Helpers._

  private val mockAuthMicroService = mock[AuthMicroService]

  when(mockAuthMicroService.authority("/auth/oid/jdensmore")).thenReturn(
    Some(UserAuthority("/personal/paye/AB123456C", Map("paye" -> "/personal/paye/AB123456C"))))

  // Configure paye service mock

  private val mockPayeMicroService = mock[PayeMicroService]

  when(mockPayeMicroService.root("/personal/paye/AB123456C")).thenReturn(
    PayeRoot(
      name = "John Densmore",
      nino = "AB123456C",
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
      Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = 898, payeNumber = "9900112", employerName = "Weyland-Yutani Corp"),
      Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = 899, payeNumber = "1212121", employerName = "Weyland-Yutani Corp")))
  )

  when(mockPayeMicroService.linkedResource[Seq[Benefit]]("/personal/paye/AB123456C/benefits/2013")).thenReturn(
    Some(Seq(
      Benefit(sequenceNumber = 1, benefitType = 30, taxYear = 2013, grossAmount = 135.33, employmentSequenceNumber = 1, cars = List()),
      Benefit(sequenceNumber = 2, benefitType = 31, taxYear = 2013, grossAmount = 22.22, employmentSequenceNumber = 3,
        cars = List(Car(sequenceNumber = 1, engineSize = 1, fuelType = 2, dateCarRegistered = new LocalDate(2011, 7, 4)))),
      Benefit(sequenceNumber = 3, benefitType = 31, taxYear = 2013, grossAmount = 321.42, employmentSequenceNumber = 2,
        cars = List(Car(sequenceNumber = 1, engineSize = 1, fuelType = 2, dateCarRegistered = new LocalDate(2012, 12, 12))))))
  )

  private def controller = new PayeController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val payeMicroService = mockPayeMicroService
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
    "display car details" in new WithApplication(FakeApplication()) {
      requestBenefits should include("Remove your company benefit")
      requestBenefits should include("Value of car benefit: £ 321.42")
    }

    "confirm successful remove benefit" in new WithApplication(FakeApplication()) {
      val requestRemoval = postRemoveBenefit("2012-06-01")
      requestRemoval should include("Your benefits have been removed from your employment")
      requestRemoval should include("June 1, 2012")
    }

    "validate date" in new WithApplication(FakeApplication()) {
      val requestRemoval = postRemoveBenefit("Flibble 1st")
      requestRemoval should include("Remove your company benefit")
      requestRemoval should include("Invalid Date")
      requestRemoval should include("format YYYY-MM-DD")
    }

    def requestBenefits = {
      val result = controller.carBenefit(3, 1)(FakeRequest().withSession(("userId", encrypt("/auth/oid/jdensmore"))))
      status(result) shouldBe 200
      contentAsString(result)
    }

    def postRemoveBenefit(date: String) = {
      val result = controller.removeCarBenefit(3, 1)(FakeRequest()
        .withSession(("userId", encrypt("/auth/oid/jdensmore")))
        .withFormUrlEncodedBody(("return_date" -> date), ("confirm" -> "checked")))
      status(result) shouldBe 200
      contentAsString(result)
    }
  }

}
