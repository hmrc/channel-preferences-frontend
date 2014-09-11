package views.sa.prefs

import controllers.sa.prefs.internal.InterstitialPageContentCohorts._
import controllers.sa.prefs.internal.PreferencesControllerHelper
import org.jsoup.Jsoup
import play.api.mvc.Call
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.test.UnitSpec
import views.html.sa.prefs.sa_printing_preference

class SaPrintingPreferenceViewSpec extends UnitSpec with PreferencesControllerHelper with WithFakeApplication {

  "preference print template" should {

    "render the correct content for the OptInNotSelected cohort " in  {
      val document = Jsoup.parse(sa_printing_preference(true, emailForm, Call("GET", "/"), OptInNotSelected)(FakeRequest("GET", "/")).toString())
      document.getElementById("opt-in-in").hasAttr("checked") shouldBe false
    }

    "render the correct content for the OptInSelected cohort " in  {
      val document = Jsoup.parse(sa_printing_preference(true, emailForm, Call("GET", "/"), OptInSelected)(FakeRequest("GET", "/")).toString())
      document.getElementById("opt-in-in").hasAttr("checked") shouldBe true
    }
  }

}
