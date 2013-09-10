package controllers

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
    "encrypt token, time and destination in a JSON and return the encrypted string with the correct destination url" in new WithApplication(FakeApplication()) {

      val validDestinationUrl = PortalConfig.destinationRoot + "/somepath"

      val result: Result = controller.encryptPayload(FakeRequest("GET", s"/ssoout?destinationUrl=$validDestinationUrl").withSession("token" -> encrypt(encodedGovernmentGatewayToken), sessionTimestampKey -> controller.now().getMillis.toString))
      status(result) should be(200)

      val content = contentAsString(result)

      val decryptedResult = SsoPayloadEncryptor.decrypt(content)
      val decryptedJson = Json.parse(decryptedResult)

      (decryptedJson \ "gw").as[String] should be(encodedGovernmentGatewayToken)
      (decryptedJson \ "dest").as[String] should be(validDestinationUrl)
      (decryptedJson \ "time").asOpt[Long].isDefined should be(true)

    }

    "when no destination url provided return an encrypt token, time and destination in a JSON and return the encrypted string with the default destination url" in new WithApplication(FakeApplication()) {

      val result: Result = controller.encryptPayload(FakeRequest("GET", s"/ssoout").withSession("token" -> encrypt(encodedGovernmentGatewayToken), sessionTimestampKey -> controller.now().getMillis.toString))
      status(result) should be(200)

      val content = contentAsString(result)

      val decryptedResult = SsoPayloadEncryptor.decrypt(content)
      val decryptedJson = Json.parse(decryptedResult)

      (decryptedJson \ "gw").as[String] should be(encodedGovernmentGatewayToken)
      (decryptedJson \ "dest").as[String] should be(PortalConfig.getDestinationUrl("home"))
      (decryptedJson \ "time").asOpt[Long].isDefined should be(true)

    }

    "when multiple destination urls are provided throw a RuntimeException" in new WithApplication(FakeApplication()) {

      val validDestinationUrl = PortalConfig.destinationRoot + "/somepath"

      val anotherValidDestinationUrl = PortalConfig.destinationRoot + "/someotherpath"
      evaluating(controller.encryptPayload(FakeRequest("GET", s"/ssoout?destinationUrl=$validDestinationUrl&destinationUrl=$anotherValidDestinationUrl")
        .withSession("token" -> encrypt(encodedGovernmentGatewayToken), sessionTimestampKey -> controller.now().getMillis.toString))) should produce[RuntimeException]

    }

    "when a destination is provided with a domain not on the approved whitelist throw a RuntimeException" in new WithApplication(FakeApplication()) {

      val invalidDestinationUrl = "www.bad.com/someotherpath"
      evaluating(controller.encryptPayload(FakeRequest("GET", s"/ssoout?destinationUrl=$invalidDestinationUrl")
        .withSession("token" -> encrypt(encodedGovernmentGatewayToken), sessionTimestampKey -> controller.now().getMillis.toString))) should produce[RuntimeException]

    }

    "when the GGW token is not present throw a RuntimeException" in new WithApplication(FakeApplication()) {

      val validDestinationUrl = PortalConfig.destinationRoot + "/somepath"

      evaluating(controller.encryptPayload(FakeRequest("GET", s"/ssoout?destinationUrl=$validDestinationUrl")
        .withSession(sessionTimestampKey -> controller.now().getMillis.toString))) should produce[RuntimeException]

    }

    //TODO BEWT: Implement logging test case
    "when a RuntimeException is thrown, log the error and the reason" in new WithApplication(FakeApplication()) {
      pending
    }
  }

}
