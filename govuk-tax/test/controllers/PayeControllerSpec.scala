package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import play.api.test.{ FakeRequest, WithApplication, FakeApplication }
import microservices.MockMicroServicesForTests
import microservice.auth.AuthMicroService
import microservice.paye.PayeMicroService
import org.mockito.Mockito._
import microservice.auth.domain.UserAuthority
import microservice.paye.domain.{ TaxCode, PayeRoot, PayeDesignatoryDetails }

class PayeControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar {

  import play.api.test.Helpers._

  private val mockAuthMicroService = mock[AuthMicroService]
  private val mockPayeMicroService = mock[PayeMicroService]

  when(mockAuthMicroService.authority("/auth/oid/jdensmore")).thenReturn(
    Some(UserAuthority(
      regimes = Map("paye" -> "/personal/paye/AB123456C"))))

  when(mockPayeMicroService.root("/personal/paye/AB123456C")).thenReturn(
    PayeRoot(
      designatoryDetails = PayeDesignatoryDetails("John", "Densmore"),
      links = Map("taxCodes" -> "/personal/paye/AB123456C/tax-codes")
    )
  )

  when(mockPayeMicroService.linkedResource[Seq[TaxCode]]("/personal/paye/AB123456C/tax-codes")).thenReturn(
    Some(Seq(TaxCode("430L")))
  )

  private def payeController = new PayeController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val payeMicroService = mockPayeMicroService
  }

  "Paye controller" should {
    "display name for John Densmore" in new WithApplication(FakeApplication()) {
      val home = payeController.home
      val result = home(FakeRequest())

      status(result) should be(200)
      contentAsString(result) should be("John Densmore")
    }

    "display the tax code for John Densmore" in new WithApplication(FakeApplication()) {
      val taxCode = payeController.taxCode
      val result = taxCode(FakeRequest())

      status(result) should be(200)
      contentAsString(result) should be("430L")
    }
  }
}
