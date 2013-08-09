package controllers

import test.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import play.api.test.{ FakeRequest, WithApplication, FakeApplication }
import microservice.{ UnauthorizedException, MockMicroServicesForTests }
import microservice.governmentgateway.{ GovernmentGatewayResponse, ValidateTokenRequest, GovernmentGatewayMicroService }
import org.mockito.Mockito._
import play.api.mvc.{ SimpleResult, Result }
import java.net.{ URI, URLEncoder }
import play.api.libs.ws.Response
import play.api.libs.json.{ Json, JsString, JsObject, JsValue }
import play.api.test.Helpers._
import microservice.governmentgateway.GovernmentGatewayResponse
import microservice.UnauthorizedException
import play.api.libs.ws.Response
import play.api.test.FakeApplication
import microservice.governmentgateway.ValidateTokenRequest
import play.api.mvc.SimpleResult
import microservice.auth.AuthMicroService
import microservice.auth.domain.{ Regimes, UserAuthority, Vrn, Utr }
import org.joda.time.DateTime
import controllers.SessionTimeoutWrapper._
import microservice.auth.domain.UserAuthority
import microservice.auth.domain.Regimes
import play.api.test.FakeApplication
import microservice.auth.domain.Utr
import scala.Some
import microservice.auth.domain.Vrn
import microservice.sa.SaMicroService
import microservice.sa.domain.SaRoot
import config.PortalConfig

class SsoOutControllerSpec extends BaseSpec with ShouldMatchers with MockitoSugar with CookieEncryption {

  private def controller = new SsoOutController

  val encodedGovernmentGatewayToken = "someEncodedToken"

  "encryptPayload" should {
    "encrypt token, time and destination in a JSON and return the encrypted string" in new WithApplication(FakeApplication()) {

      val result: Result = controller.encryptPayload(FakeRequest("GET", s"/ssoout").withSession("token" -> encrypt(encodedGovernmentGatewayToken), sessionTimestampKey -> controller.now().getMillis.toString))
      status(result) should be(200)

      val content = contentAsString(result)

      val decryptedResult = SsoPayloadEncryptor.decrypt(content)
      val decryptedJson = Json.parse(decryptedResult)

      (decryptedJson \ "gw").as[String] must be(encodedGovernmentGatewayToken)
      (decryptedJson \ "dest").as[String] must be(PortalConfig.ssoUrl)
      (decryptedJson \ "time").asOpt[Long].isDefined must be(true)

    }
  }

}
