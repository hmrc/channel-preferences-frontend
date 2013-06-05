package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import play.api.test.{ FakeRequest, WithApplication, FakeApplication }
import microservices.MockMicroServicesForTests
import microservice.auth.AuthMicroService
import microservice.personaltax.PayeMicroService
import org.mockito.Mockito._
import microservice.auth.domain.MatsUserAuthority
import controllers.domain.{ PayeDesignatoryDetails, PayeRoot }
import play.api.mvc.AsyncResult

class PayeControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar {

  import play.api.test.Helpers._

  private val mockAuthMicroService = mock[AuthMicroService]
  private val mockPayeMicroService = mock[PayeMicroService]

  when(mockAuthMicroService.authority("/auth/oid/jdensmore")).thenReturn(
    MatsUserAuthority(
      regimes = Map("paye" -> "/personal/paye/AB123456C")))

  when(mockPayeMicroService.root("/personal/paye/AB123456C")).thenReturn(
    PayeRoot(
      designatoryDetails = PayeDesignatoryDetails(name = "John Densmore"),
      links = Map.empty
    )
  )

  private val payeController = new PayeController with MockMicroServicesForTests {
    override val authMicroService = mockAuthMicroService
    override val payeMicroService = mockPayeMicroService
  }

  "Paye controller" should {
    "display name for john densmore" in new WithApplication(FakeApplication()) {
      val home = payeController.home
      val asyncResult = home(FakeRequest()).asInstanceOf[AsyncResult]

      val result = await(asyncResult.result, 1)

      status(result) should be(200)
      contentAsString(result) should be("John Densmore")
    }
  }
}
