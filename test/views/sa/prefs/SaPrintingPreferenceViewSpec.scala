package views.sa.prefs

import controllers.sa.prefs.internal.PreferencesControllerHelper
import org.jsoup.Jsoup
import org.openqa.selenium.internal.seleniumemulation.GetSelectOptions
import play.api.i18n.Messages
import play.api.mvc.Call
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.test.UnitSpec
import views.html.sa.prefs.sa_printing_preference
import controllers.sa.prefs.internal.InterstitialPageContentCohorts._

class SaPrintingPreferenceViewSpec extends UnitSpec with PreferencesControllerHelper with WithFakeApplication {

  "preference print template" should {

    "render the correct content for the SignUpForSelfAssesment cohort " in  {
      val document = Jsoup.parse(sa_printing_preference(true, emailForm, Call("GET", "/"), SignUpForSelfAssesment)(FakeRequest("GET", "/")).toString())
      document.getElementsByTag("h1").text should include (Messages("sa_printing_preference.SignUpForSelfAssesment.heading"))
    }

    "render the correct content for the GetSelfAssesment cohort " in  {
      val document = Jsoup.parse(sa_printing_preference(true, emailForm, Call("GET", "/"), GetSelfAssesment)(FakeRequest("GET", "/")).toString())
      document.getElementsByTag("h1").text should include (Messages("sa_printing_preference.GetSelfAssesment.heading"))
    }
  }

}
