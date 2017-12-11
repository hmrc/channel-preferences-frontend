package views.sa.prefs

import _root_.helpers.ConfigHelper
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import views.html.sa.prefs.sa_printing_preference_wrong_token

class SaPrintingPreferenceWrongTokenSpec  extends UnitSpec with OneAppPerSuite {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "sa printing preferences wrong token template" should {
    "render the correct content in english" in {
      val document = Jsoup.parse(sa_printing_preference_wrong_token()(FakeRequest("GET", "/"), applicationMessages).toString())

      document.getElementsByTag("title").first().text() shouldBe "You've used a link that has now expired"
    }
  }
}
