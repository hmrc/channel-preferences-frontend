package controllers

import test.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.scalatest.matchers.ShouldMatchers
import play.api.mvc.Controller
import microservice.auth.AuthMicroService
import microservice.paye.PayeMicroService
import org.mockito.Mockito.when
import microservice.auth.domain.UserAuthority
import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import play.api.test.Helpers._
import microservices.MockMicroServicesForTests
import microservice.paye.domain.{ PayeRegime, PayeRoot }

class AuthorisedForActionSpec extends BaseSpec with ShouldMatchers with MockitoSugar {

  private val mockAuthMicroService = mock[AuthMicroService]
  private val mockPayeMicroService = mock[PayeMicroService]

  when(mockPayeMicroService.root("/personal/paye/AB123456C")).thenReturn(
    PayeRoot(
      name = "John Densmore",
      links = Map.empty
    )
  )

  object TestController extends Controller with ActionWrappers with MockMicroServicesForTests {

    override val authMicroService = mockAuthMicroService
    override val payeMicroService = mockPayeMicroService

    def test = AuthorisedForAction[PayeRegime] {
      implicit user =>
        implicit request =>
          val userPayeRegimeRoot = user.regime.paye.getOrElse(throw new Exception("No PAYE regime for user"))
          val userName = userPayeRegimeRoot.name
          Ok(userName)
    }
  }

  "basic homepage test" should {
    "contain the user's first name in the response" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/jdensmore")).thenReturn(
        Some(UserAuthority(regimes = Map("paye" -> "/personal/paye/AB123456C"))))

      val result = TestController.test(FakeRequest())

      status(result) should equal(200)
      contentAsString(result) should include("John Densmore")
    }
  }

  "AuthorisedForAction" should {
    "return Unauthorised if no Authority is returned from the Auth service" in new WithApplication(FakeApplication()) {
      when(mockAuthMicroService.authority("/auth/oid/jdensmore")).thenReturn(None)

      val result = TestController.test(FakeRequest())
      status(result) should equal(401)
    }
  }

}
