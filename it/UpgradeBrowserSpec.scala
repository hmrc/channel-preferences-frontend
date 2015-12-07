import java.net.URLEncoder

import org.openqa.selenium.WebDriver
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.{Cookie, Cookies, Session}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.endtoend
import uk.gov.hmrc.endtoend.sa.config.TestConfig
import uk.gov.hmrc.endtoend.sa.page.{Page, PreferencesFrontendPage}
import uk.gov.hmrc.test.it.BearerToken

//TODO rename Spec & package
class UpgradeBrowserSpec extends endtoend.sa.Spec with ScalaFutures with BrowserSessionCookie with Stubs with ServerSetup {

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
      val utr = "1111111111"

      Given("I am logged in")
        Auth.stubLoginPage(cookieFor(BearerToken("1234567890"), userId = "/auth/oid/1234567890"))
        go to "http://localhost:8080/login"
      Auth.stubAuth()

      Given("I have my preferences set")
        Preferences.stubPreference(utr)
        Redirect.stubRedirectPage

      Given("I am on the Upgrade Page")
        go to GenericUpgradePage
        GenericUpgradePage should be (displayed)


      When("I click 'Yes' and then 'Submit")
        click on GenericUpgradePage.`terms and conditions checkbox`
        click on GenericUpgradePage.`continue`


      Then("I am taken back to the return page")
        RedirectedPage should be (displayed)


      And("My T&Cs have been set to generic=accepted")
        Preferences.verifyPostTermsAndCondition(utr, genericAccepted = true)
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

  def cookieFor(bearerToken: BearerToken, authProvider: String = "GGW", userId: String): Cookie = {
    val keyValues = Map(
      "authToken" -> bearerToken.token,
      "token" -> "system-assumes-valid-token",
      "userId" -> userId,
      "ap" -> authProvider
    )
    Cookie(name = "mdtp", value = ApplicationCrypto.SessionCookieCrypto.encrypt(PlainText(Session.encode(keyValues))).value)
  }
}



