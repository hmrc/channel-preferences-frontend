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

class PayeControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar {

  import play.api.test.Helpers._

  private val mockAuthMicroService = mock[AuthMicroService]

  when(mockAuthMicroService.authority("/auth/oid/jdensmore")).thenReturn(
    Some(UserAuthority(
      regimes = Map("paye" -> "/personal/paye/AB123456C"))))

  private val mockPayeMicroService = mock[PayeMicroService]

  when(mockPayeMicroService.root("/personal/paye/AB123456C")).thenReturn(
    PayeRoot(
      name = "John Densmore",
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
    Some(Seq(Employment(sequenceNumber = 1, startDate = "02/07/2013", endDate = "08/10/2013", taxDistrictNumber = "898", payeNumber = "9900112")))
  )

  when(mockPayeMicroService.linkedResource[Seq[Benefit]]("/personal/paye/AB123456C/benefits/2013")).thenReturn(
    Some(Seq(Benefit(taxYear = "2102", grossAmount = 13533, employmentSequenceNumber = 1), Benefit(taxYear = "2013", grossAmount = 2222, employmentSequenceNumber = 2)))
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
    }

    "return the link to the list of benefits for John Densmore" in new WithApplication(FakeApplication()) {
      val content = requestHome
      content should include("Click here to see your benefits")
    }

    def requestHome: String = {
      val home = controller.home
      val result = home(FakeRequest())

      status(result) should be(200)

      contentAsString(result)
    }
  }

  "The benefits method" should {

    "return John's benefits" in new WithApplication(FakeApplication()) {
      requestBenefits should include("13533")
    }

    "not return a benefits without a corresponding employment" in new WithApplication(FakeApplication()) {
      requestBenefits should not include "2222"
    }

    def requestBenefits = {
      val result = controller.benefits(FakeRequest())
      status(result) shouldBe 200
      contentAsString(result)
    }

  }

}
