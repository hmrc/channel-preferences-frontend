package controllers

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import play.api.test.{ FakeRequest, WithApplication, FakeApplication }
import uk.gov.hmrc.microservice.UnauthorizedException
import uk.gov.hmrc.microservice.governmentgateway.{ GovernmentGatewayResponse, ValidateTokenRequest, GovernmentGatewayMicroService }
import org.mockito.Mockito._
import play.api.mvc.{ SimpleResult, Result }
import java.net.{ URI, URLEncoder }
import play.api.libs.ws.Response
import play.api.libs.json.{ Json, JsString, JsObject, JsValue }
import play.api.test.Helpers._
import uk.gov.hmrc.microservice.governmentgateway.GovernmentGatewayResponse
import uk.gov.hmrc.microservice.UnauthorizedException
import play.api.libs.ws.Response
import play.api.test.FakeApplication
import uk.gov.hmrc.microservice.governmentgateway.ValidateTokenRequest
import play.api.mvc.SimpleResult
import uk.gov.hmrc.microservice.auth.AuthMicroService
import uk.gov.hmrc.microservice.auth.domain.{ Regimes, UserAuthority, Vrn, Utr }
import org.joda.time.DateTime
import controllers.SessionTimeoutWrapper._
import uk.gov.hmrc.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.microservice.auth.domain.Regimes
import play.api.test.FakeApplication
import uk.gov.hmrc.microservice.auth.domain.Utr
import scala.Some
import uk.gov.hmrc.microservice.auth.domain.Vrn
import uk.gov.hmrc.microservice.sa.SaMicroService
import uk.gov.hmrc.microservice.sa.domain.SaRoot
import config.PortalConfig
import uk.gov.hmrc.common.BaseSpec

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
      (decryptedJson \ "dest").as[String] must be(PortalConfig.destinationRoot + "/home")
      (decryptedJson \ "time").asOpt[Long].isDefined must be(true)

    }
  }

}
