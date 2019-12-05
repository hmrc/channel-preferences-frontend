package views.sa.prefs

import _root_.helpers.{ConfigHelper, WelshLanguage}
import controllers.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import views.html.sa.prefs.sa_printing_preference_verify_email_failed

class SaPrintingPreferenceVerifyEmailFailedSpec extends UnitSpec with OneAppPerSuite with WelshLanguage {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "printing preferences verify email failed template" should {
    "render the correct content in english" in {
      val document = Jsoup.parse(sa_printing_preference_verify_email_failed(None, None)(AuthenticatedRequest(FakeRequest("GET", "/"), None, None, None, None), applicationMessages).toString())

      document.getElementsByTag("title").first().text() shouldBe "Email address already verified"
    }

    "render the correct content in welsh" in {
      val document = Jsoup.parse(sa_printing_preference_verify_email_failed(None, None)(welshRequest, messagesInWelsh(applicationMessages)).toString())

      document.getElementsByTag("title").first().text() shouldBe "Cyfeiriad e-bost wedi'i ddilysu eisoes"
      document.getElementById("failure-heading").text() shouldBe "Cyfeiriad e-bost wedi'i ddilysu eisoes"
      document.getElementById("success-message").text() shouldBe "Mae'ch cyfeiriad e-bost wedi'i ddilysu eisoes."
      document.getElementById("link-to-home").child(0).text() shouldBe "Yn eich blaen i'ch cyfrif ar-lein gyda CThEM"
    }
  }
}
