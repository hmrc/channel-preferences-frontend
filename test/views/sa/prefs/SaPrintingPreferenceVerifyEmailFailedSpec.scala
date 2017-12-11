package views.sa.prefs

import _root_.helpers.ConfigHelper
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import views.html.sa.prefs.sa_printing_preference_verify_email_failed

class SaPrintingPreferenceVerifyEmailFailedSpec extends UnitSpec with OneAppPerSuite {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "preference print template" should {
    "render the correct content for the IPage cohort " in {
      val document = Jsoup.parse(sa_printing_preference_verify_email_failed(None, None)(FakeRequest("GET", "/"), applicationMessages).toString())

      document.getElementsByTag("title").first().text() shouldBe "Email address already verified"
    }
  }
}