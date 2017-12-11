package views.includes

import helpers.ConfigHelper
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import views.html.includes.last_sign_in

class LastSignInSpec extends UnitSpec with OneAppPerSuite {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "Header Nav Links" should {
    "Display the sign out text from messages" in {
      val document = Jsoup.parse(last_sign_in(new DateTime(), None)(FakeRequest("GET", "/"), applicationMessages).toString())
      document.getElementsByClass("last-login__more-details").first().text() should (startWith ("Last sign in:") and endWith ("not right?"))
      document.getElementsByClass("minimise").first().text() shouldBe "Minimise"
      document.getElementsByClass("alert__message").first().text() should startWith ("Last sign in:")
      document.getElementsByAttributeValue("href", "https://www.gov.uk/hmrc-online-services-helpdesk").text() shouldBe ("Contact HMRC")
      document.getElementsByClass("flush--bottom").first().text() should endWith ("if it wasn't you.")
    }
  }
}