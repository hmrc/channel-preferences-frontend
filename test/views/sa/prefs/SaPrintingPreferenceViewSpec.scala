package views.sa.prefs

import controllers.sa.prefs.internal._
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
      document.getElementsByTag("title").text should include ("Sign up for")
    }

    "render the content for CPage cohort" in {
      val document = Jsoup.parse(sa_printing_preference(true, emailForm, Call("GET", "/"), CPage)(FakeRequest("GET", "/")).toString())
      document.getElementsByTag("title").text should be ("Self Assessment email reminders from HMRC")
    }

    "render the content for DPage cohort" in {
      val document = Jsoup.parse(sa_printing_preference(true, emailForm, Call("GET", "/"), DPage)(FakeRequest("GET", "/")).toString())
      document.select("#content h1").text should include ("go paperless")
    }

    "render the content for EPage cohort" in {
      val document = Jsoup.parse(sa_printing_preference(true, emailForm, Call("GET", "/"), EPage)(FakeRequest("GET", "/")).toString())
      document.select("#content h1").text should be ("Get paperless services first")
      }
  }
}
