import java.net.URLEncoder

import com.github.tomakehurst.wiremock.client.WireMock._
import org.openqa.selenium.WebDriver
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.{Cookie, Session}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.endtoend
import uk.gov.hmrc.endtoend.sa.{Page, RelativeUrl, ToAbsoluteUrl}
import uk.gov.hmrc.test.it.BearerToken

//TODO rename Spec & package
class UpgradeBrowserSpec extends endtoend.sa.Spec with ScalaFutures with BrowserSessionCookie with ServerSetup {
  implicit def internalPageUrls[T <: InternalPage] = ToAbsoluteUrl.fromRelativeUrl[T](host = "localhost", port = 9000)
  implicit def stubbedUrls[T <: Stubbed with RelativeUrl] = ToAbsoluteUrl.fromRelativeUrl[T](host = "localhost", port = 8080)

  feature("The generic upgrade page") {
    scenario("Agree to upgrading") {
      val utr = "1111111111"

      Given("I am logged in")
        go to Stub.Auth.loginPage(cookieFor(BearerToken("1234567890"), userId = "/auth/oid/1234567890"))
        givenThat (Stub.Auth.`GET /auth/authority` willReturn (aResponse withStatus 200 withBody Stub.Auth.authorityRecordJson))

      And("I have my preferences set")
        givenThat (Stub.Preferences.`GET /preferences/sa/individual/<utr>/print-suppression`(utr) willReturn (
          aResponse withStatus 200 withBody Stub.Preferences.optedInPreferenceJson
        ))

      And("I am on the Upgrade Page")
        val upgradePage = GenericUpgradePage(returnUrl = Stub.Host.ReturnPage)
        go to upgradePage
        upgradePage should be (displayed)

      When("I click 'Yes' and then 'Submit")
        click on upgradePage.`terms and conditions checkbox`
        click on upgradePage.`continue`

      Then("I am taken back to the return page")
        Stub.Host.ReturnPage should be (displayed)

      And("My T&Cs have been set to generic=accepted")
        verify(Stub.Preferences.`POST /preferences/sa/individual/<utr>/terms-and-conditions`(utr)(
          genericAccepted = true
        ))
    }
  }
}

trait InternalPage extends Page
trait Stubbed

object GenericUpgradePage {
  def apply[T](returnUrl: T)(implicit toAbsoluteUrl: ToAbsoluteUrl[T]) = new InternalPage {
    val title = "Go paperless with HMRC"

    def relativeUrl = "account/account-details/sa/upgrade-email-reminders?returnUrl=" +
      URLEncoder.encode(ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText(toAbsoluteUrl.absoluteUrl(returnUrl))).value, "utf8")

    def `terms and conditions checkbox`(implicit driver: WebDriver) = checkbox("accept-tc").underlying
    def `no ask me later radio button`(implicit driver: WebDriver) = radioButton("opt-in-out").underlying
    def `yes continue electronic comms radio button`(implicit driver: WebDriver) = radioButton("opt-in-in").underlying
    def continue(implicit driver: WebDriver) = id("submitUpgrade")
  }
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