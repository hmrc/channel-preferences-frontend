package views.sa.prefs

import _root_.helpers.{ConfigHelper, WelshLanguage}
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import views.html.sa.prefs.sa_printing_preference_verify_email

class SaPrintingPreferenceVerifyEmailSpec extends UnitSpec with OneAppPerSuite with WelshLanguage {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "printing preferences verify email template" should {
    "render the correct content in english" in {
      val document = Jsoup.parse(sa_printing_preference_verify_email(None, None)(FakeRequest("GET", "/"), langEn, applicationMessages).toString())

      document.getElementsByTag("title").first().text() shouldBe "Email address verified"
    }

    "render the correct content in welsh" in {
      val document = Jsoup.parse(sa_printing_preference_verify_email(None, None)(welshRequest, langCy, messagesInWelsh(applicationMessages)).toString())

      document.getElementsByTag("title").first().text() shouldBe "Cyfeiriad e-bost wedi'i ddilysu"
      document.getElementById("success-heading").text() shouldBe "Cyfeiriad e-bost wedi'i ddilysu"
      document.getElementById("success-message").text() shouldBe "Rydych nawr wedi cofrestru ar gyfer hysbysiadau di-bapur."
      document.getElementById("link-to-home").child(0).text() shouldBe "Yn eich blaen i'ch cyfrif ar-lein gyda CThEM"
    }
  }
}