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

class HomeControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar {

  import play.api.test.Helpers._

  private val mockAuthMicroService = mock[AuthMicroService]
  private val mockPayeMicroService = mock[PayeMicroService]

  when(mockAuthMicroService.authority("/auth/oid/jdensmore")).thenReturn(
    Some(UserAuthority(
      regimes = Map("paye" -> "/personal/paye/AB123456C"))))

  when(mockPayeMicroService.root("/personal/paye/AB123456C")).thenReturn(
    PayeRoot(
      designatoryDetails = PayeDesignatoryDetails(name = "John Densmore"),
      links = Map("taxCodes" -> "/personal/paye/AB123456C/tax-codes")
    )
  )

  when(mockPayeMicroService.taxCodes("/personal/paye/AB123456C/tax-codes")).thenReturn(
    Some(Seq(TaxCode("430L")))
  )

  private def controller = new HomeController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val payeMicroService = mockPayeMicroService
  }

  "The home method" should {

    "return the name for John Densmore" in new WithApplication(FakeApplication()) {
      val content = requestHome
      content should include("John Densmore")
    }

    "return the tax code for John Densmore" in new WithApplication(FakeApplication()) {
      val content = requestHome
      content should include("430L")
    }

    def requestHome: String = {
      val home = controller.home
      val result = home(FakeRequest())

      status(result) should be(200)

      contentAsString(result)
    }
  }
}
