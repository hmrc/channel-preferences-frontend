import java.net.URLEncoder

import com.github.tomakehurst.wiremock.WireMockServer
import org.openqa.selenium.WebDriver
import org.scalatest.concurrent.ScalaFutures
import play.api.http.HeaderNames
import play.api.libs.ws.WS
import play.api.mvc.{Session, Cookie, Cookies}
import play.api.test.TestServer
import uk.gov.hmrc.crypto.{PlainText, ApplicationCrypto}
import uk.gov.hmrc.endtoend
import uk.gov.hmrc.endtoend.sa.config.TestConfig
//import uk.gov.hmrc.endtoend.sa.page.PreferencesFrontEnd.GenericUpgradePage._
//import uk.gov.hmrc.endtoend.sa.page.PreferencesFrontEnd._
import uk.gov.hmrc.endtoend.sa.page.{Page, PreferencesFrontendPage}
import uk.gov.hmrc.test.it.{BearerToken, FrontendCookieHelper}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

import scala.concurrent.Future

//TODO rename Spec & package
class UpgradeBrowserSpec extends endtoend.sa.Spec with ScalaFutures with BrowserSessionCookie with AuthenticationBaker {

  val t = TestServer(9000)

  override def beforeAll() = {
    super.beforeAll()
    t.start()

    val wireMockServer = new WireMockServer(wireMockConfig().port(8080))
    wireMockServer.start()
  }

  override def afterAll() = {
    super.afterAll()
    t.stop()
  }

  implicit val testConfig = TestConfig(
    frontendBaseUrl = "http://localhost:9000",
    requiresPort = false,
    //TODO remove all of the following args
    saUser = null,
    nonSAUser = null,
    ninoUser = null,
    apiBaseUrl = "",
    mailgunBaseUrl = ""
  )

  feature("The generic upgrade page") {
    scenario("Agree to upgrading") {
      Given("I am on the Upgrade Page")

      val session = cookieFor(BearerToken("1234567890"), userId = "/auth/oid/1234567890")
      stubLogin(Cookies.encode(Seq(Cookie(session._1, session._2))))

      go to "http://localhost:8080/login"

      stubAuth()
      stubPreference("1111111111")
      stubRedirectPage

      go to GenericUpgradePage
      GenericUpgradePage should be (displayed)

      When("I click 'Yes' and then 'Submit")
        click on GenericUpgradePage.`terms and conditions checkbox`
        click on GenericUpgradePage.`continue`


      Then("I am taken back to the return page")
        RedirectedPage should be (displayed)


      And("My T&Cs have been set to generic=accepted")
        verifyTermsAndCondition("1111111111")
    }
  }
}

object GenericUpgradePage extends PreferencesFrontendPage {
  val title = "Go paperless with HMRC"
  def relativeUrl(implicit testConfig: TestConfig) = "account/account-details/sa/upgrade-email-reminders?returnUrl=" +
    URLEncoder.encode(ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText("http://localhost:8080/some/other/page")).value, "utf8")

  def `terms and conditions checkbox`(implicit driver: WebDriver) =              checkbox("accept-tc").underlying
  def `no ask me later radio button`(implicit driver: WebDriver) =               radioButton("opt-in-out").underlying
  def `yes continue electronic comms radio button`(implicit driver: WebDriver) = radioButton("opt-in-in").underlying
  def continue(implicit driver: WebDriver) =                                     id("submitUpgrade")
}

object RedirectedPage extends Page {
  val title = "Redirected Page"

  def relativeUrl(implicit testConfig: TestConfig) = "/some/other/page"

  def port = 8080
}

//TODO tidy up creating cookies
trait BrowserSessionCookie {

  def cookieFor(bearerToken: BearerToken, authProvider: String = "GGW", userId: String): (String, String) = {
    val keyValues = Map(
      "authToken" -> bearerToken.token,
      "token" -> "system-assumes-valid-token",
      "userId" -> userId,
      "ap" -> authProvider
    )
    "mdtp" -> ApplicationCrypto.SessionCookieCrypto.encrypt(PlainText(Session.encode(keyValues))).value
  }
}

// TODO: Tidy up into own set of suite classes
trait AuthenticationBaker {
  // /preferences/sa/individual/$utr/print-suppression
  def stubPreference(utr: String) = stubFor(get(urlEqualTo(s"/preferences/sa/individual/$utr/print-suppression"))
    .willReturn(
      aResponse()
        .withStatus(200)
        .withBody(
          s"""
             |{
             |    "digital": true
             |}
            """.stripMargin
        )))

  def stubLogin(cookieValue: String) =
    stubFor(get(urlEqualTo("/login"))
      .willReturn(
        aResponse()
          .withHeader(HeaderNames.SET_COOKIE, cookieValue)
          .withStatus(200)
      ))

  def stubAuth() =
    stubFor(get(urlEqualTo("/auth/authority"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               |    "uri": "/auth/oid/1234567890",
               |    "loggedInAt": "2014-06-09T14:57:09.522Z",
               |    "accounts": {
               |       "sa": {
               |        "link": "/sa/individual/1111111111",
               |        "utr": "1111111111"
               |       }
               |    },
               |    "levelOfAssurance": "2",
               |    "confidenceLevel": 50
               |}
            """.stripMargin
          )))

  def stubRedirectPage = stubFor(get(urlEqualTo(s"/some/other/page"))
    .willReturn(
      aResponse()
        .withStatus(200)
        .withHeader("X-Title", "Redirected Page")
        .withBody(
          "<html><head><title>Redirected Page</title></head></html>"
        )))

  def verifyTermsAndCondition(utr: String) =
    verify(postRequestedFor(urlEqualTo(s"/preferences/sa/individual/$utr/terms-and-conditions"))
      .withRequestBody(equalToJson(
        s"""{ "generic": {
           |  "accepted": true
           |  }
           |}
         """.stripMargin)))
}