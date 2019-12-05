package views.includes

import controllers.auth.AuthenticatedRequest
import helpers.{ConfigHelper, WelshLanguage}
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import views.html.includes.last_sign_in

class LastSignInSpec extends UnitSpec with OneAppPerSuite with WelshLanguage {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "Last sign in template" should {
    "render the correct content in english" in {
      val document = Jsoup.parse(last_sign_in(new DateTime(), None)(AuthenticatedRequest(FakeRequest("GET", "/"), None, None, None, None), applicationMessages).toString())
      document.getElementsByClass("last-login__more-details").first().text() should (startWith("Last sign in:") and endWith("not right?"))
      document.getElementsByClass("minimise").first().text() shouldBe "Minimise"
      document.getElementsByClass("alert__message").first().text() should startWith("Last sign in:")
      document.getElementsByAttributeValue("href", "https://www.gov.uk/hmrc-online-services-helpdesk").text() shouldBe ("Contact HMRC")
      document.getElementsByClass("flush--bottom").first().text() should endWith("if it wasn't you.")
    }

    "render the correct content in welsh" in {
      val document = Jsoup.parse(last_sign_in(new DateTime(), None)(welshRequest, messagesInWelsh(applicationMessages)).toString())
      document.getElementsByClass("last-login__more-details").first().text() should (startWith("Mewngofnodwyd diwethaf:") and endWith("dim yn gywir?"))
      document.getElementsByClass("minimise").first().text() shouldBe "Lleihau"
      document.getElementsByClass("alert__message").first().text() should startWith("Mewngofnodwyd diwethaf:")
      document.getElementsByAttributeValue("href", "https://www.gov.uk/hmrc-online-services-helpdesk").text() shouldBe ("Cysylltwch â CThEM")
      document.getElementsByClass("flush--bottom").first().text() should endWith("os nad chi ydoedd.")
    }
  }
}
