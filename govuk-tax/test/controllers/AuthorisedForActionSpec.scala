package controllers

import test.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.scalatest.matchers.ShouldMatchers
import play.api.mvc.{ AsyncResult, Controller }
import scala.concurrent.Future
import controllers.domain.{ PayeDesignatoryDetails, PayeRoot, PayeRegime }
import microservice.auth.AuthMicroService
import microservice.paye.PayeMicroService
import org.mockito.Mockito.when
import microservice.auth.domain.MatsUserAuthority
import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits._
import microservices.MockMicroServicesForTests

class AuthorisedForActionSpec extends BaseSpec with ShouldMatchers with MockitoSugar {

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

  object TestController extends Controller with ActionWrappers with MockMicroServicesForTests {

    override val authMicroService = mockAuthMicroService
    override val payeMicroService = mockPayeMicroService

    def test = AuthorisedForAction[PayeRegime] {
      implicit user =>
        implicit request =>
          Async {
            val userPayeRegimeRoot = user.regime.paye.getOrElse(throw new Exception("No PAYE regime for user"))
            val userName = userPayeRegimeRoot.designatoryDetails.name

            Future(Ok(userName))
          }
    }
  }

  "basic homepage test" should {
    "contain the user's first name in the response" in new WithApplication(FakeApplication()) {
      val asyncResult = TestController.test(FakeRequest()).asInstanceOf[AsyncResult]
      val result = await(asyncResult.result, 1)

      status(result) should equal(200)
      contentAsString(result) should include("John Densmore")
    }
  }

}
