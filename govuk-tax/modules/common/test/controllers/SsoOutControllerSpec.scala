package controllers

import org.scalatest.mock.MockitoSugar
import play.api.test.{ FakeRequest, WithApplication }
import play.api.libs.json.Json
import play.api.test.Helpers._
import controllers.common._
import play.api.test.FakeApplication
import config.{DateTimeProvider, PortalConfig}
import uk.gov.hmrc.common.BaseSpec
import org.joda.time.DateTime
import common.preferences.service._

class SsoOutControllerSpec extends BaseSpec with MockitoSugar {

  private def dateTime = DateTimeProvider.now
  private def controller = new SsoOutController() {
    override def now: () => DateTime = dateTime
  }

  private def sessionTimeout = dateTime().getMillis.toString
  
  val encodedGovernmentGatewayToken = "someEncodedToken"

  "encryptPayload" should {
    "encrypt token, time and destination in a JSON and return the encrypted string with the correct destination url" in new WithApplication(FakeApplication()) {

      val validDestinationUrl = PortalConfig.destinationRoot + "/somepath"

      val result = controller.encryptPayload(FakeRequest("GET", s"/ssoout?destinationUrl=$validDestinationUrl").withSession(SessionKeys.token -> encodedGovernmentGatewayToken, SessionKeys.lastRequestTimestamp -> sessionTimeout))
      status(result) should be(200)

      val content = contentAsString(result)

      val decryptedResult = SsoPayloadCrypto.decrypt(content)
      val decryptedJson = Json.parse(decryptedResult)

      (decryptedJson \ "gw").as[String] should be(encodedGovernmentGatewayToken)
      (decryptedJson \ "dest").as[String] should be(validDestinationUrl)
      (decryptedJson \ "time").asOpt[Long].isDefined should be(true)

    }

    "when no destination url provided return an encrypt token, time and destination in a JSON and return the encrypted string with the default destination url" in new WithApplication(FakeApplication()) {

      val result = controller.encryptPayload(FakeRequest("GET", s"/ssoout").withSession(SessionKeys.token -> encodedGovernmentGatewayToken, SessionKeys.lastRequestTimestamp -> sessionTimeout))
      status(result) should be(200)

      val content = contentAsString(result)

      val decryptedResult = SsoPayloadCrypto.decrypt(content)
      val decryptedJson = Json.parse(decryptedResult)

      (decryptedJson \ "gw").as[String] should be(encodedGovernmentGatewayToken)
      (decryptedJson \ "dest").as[String] should be(PortalConfig.getDestinationUrl("home"))
      (decryptedJson \ "time").asOpt[Long].isDefined should be(true)

    }

    "when multiple destination urls are provided return a BadRequest" in new WithApplication(FakeApplication()) {

      val validDestinationUrl = PortalConfig.destinationRoot + "/somepath"

      val anotherValidDestinationUrl = PortalConfig.destinationRoot + "/someotherpath"
      val response = controller.encryptPayload(FakeRequest("GET", s"/ssoout?destinationUrl=$validDestinationUrl&destinationUrl=$anotherValidDestinationUrl")
        .withSession(SessionKeys.token -> encodedGovernmentGatewayToken, SessionKeys.lastRequestTimestamp -> sessionTimeout))
      status(response) shouldBe 400
    }

    "when a destination is provided with a domain not on the approved whitelist return a BadRequest" in new WithApplication(FakeApplication()) {

      val invalidDestinationUrl = "www.bad.com/someotherpath"
      val response = controller.encryptPayload(FakeRequest("GET", s"/ssoout?destinationUrl=$invalidDestinationUrl")
        .withSession(SessionKeys.token -> encodedGovernmentGatewayToken, SessionKeys.lastRequestTimestamp -> sessionTimeout))
      status(response) shouldBe 400
    }

    "when the GGW token is not present return a BadRequest" in new WithApplication(FakeApplication()) {

      val validDestinationUrl = PortalConfig.destinationRoot + "/somepath"

      val response = controller.encryptPayload(FakeRequest("GET", s"/ssoout?destinationUrl=$validDestinationUrl")
        .withSession(SessionKeys.lastRequestTimestamp -> sessionTimeout))
      status(response) shouldBe 400
    }

  }

}
