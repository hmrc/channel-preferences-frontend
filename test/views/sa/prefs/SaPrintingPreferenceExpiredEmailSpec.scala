package views.sa.prefs

import _root_.helpers.{ConfigHelper, WelshLanguage}
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import views.html.sa.prefs.sa_printing_preference_expired_email

class SaPrintingPreferenceExpiredEmailSpec extends UnitSpec with OneAppPerSuite with WelshLanguage {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "printing preferences expired emai; template" should {
    "render the correct content in english" in {
      val document = Jsoup.parse(sa_printing_preference_expired_email()(FakeRequest("GET", "/"), applicationMessages).toString())

      document.getElementsByTag("title").first().text() shouldBe "Your email address is NOT verified"
      document.getElementById("link-to-home").childNodes().get(1).toString.trim() shouldBe "and request a new verification link"
    }

    "render the correct content in welsh" in {
      val document = Jsoup.parse(sa_printing_preference_expired_email()(welshRequest, messagesInWelsh(applicationMessages)).toString())

      document.getElementsByTag("title").first().text() shouldBe "NID yw'ch cyfeiriad e-bost wedi'i ddilysu"
      document.getElementById("link-to-home").childNodes().get(0).childNode(0).toString shouldBe "Yn eich blaen i'ch cyfrif ar-lein gyda CThEM"
//TODO :      document.getElementById("link-to-home").childNodes().get(1).toString.trim() shouldBe "and request a new verification link WELSH"
    }
  }
}