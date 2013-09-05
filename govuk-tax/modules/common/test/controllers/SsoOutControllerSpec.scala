package controllers

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import play.api.test.{ FakeRequest, WithApplication }
import play.api.mvc.Result
import play.api.libs.json.Json
import play.api.test.Helpers._
import controllers.common._
import SessionTimeoutWrapper._
import play.api.test.FakeApplication
import config.PortalConfig
import uk.gov.hmrc.common.BaseSpec

class SsoOutControllerSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  private def controller = new SsoOutController

  val encodedGovernmentGatewayToken = "someEncodedToken"

  "encryptPayload" should {
    "encrypt token, time and destination in a JSON and return the encrypted string" in new WithApplication(FakeApplication()) {

      val result: Result = controller.encryptPayload(FakeRequest("GET", s"/ssoout").withSession("token" -> encrypt(encodedGovernmentGatewayToken), sessionTimestampKey -> controller.now().getMillis.toString))
      status(result) should be(200)

      val content = contentAsString(result)

      val decryptedResult = SsoPayloadEncryptor.decrypt(content)
      val decryptedJson = Json.parse(decryptedResult)

      (decryptedJson \ "gw").as[String] should be(encodedGovernmentGatewayToken)
      (decryptedJson \ "dest").as[String] should be(PortalConfig.destinationRoot + "/home")
      (decryptedJson \ "time").asOpt[Long].isDefined should be(true)

    }
  }

}
