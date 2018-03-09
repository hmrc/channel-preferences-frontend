package views.sa.prefs

import _root_.helpers.ConfigHelper
import controllers.internal._
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Lang
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Call
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import views.html.sa.prefs.sa_printing_preference

class SaPrintingPreferenceViewSpec extends UnitSpec with OneAppPerSuite {

  override implicit lazy val app : Application = ConfigHelper.fakeApp

  "preference print template" should {
    "render the correct content for the IPage cohort " in {
      val document = Jsoup.parse(sa_printing_preference(
        emailForm = EmailForm(),
        submitPrefsFormAction = Call("GET", "/"),
        cohort = IPage
      )(FakeRequest("GET", "/"), applicationMessages).toString())

      document.getElementById("opt-in-in").hasAttr("checked") shouldBe true
    }
  }
}
