package views.sa.prefs

import _root_.helpers.ConfigHelper
import controllers.internal._
import model.Encrypted
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import views.html.sa.prefs.{upgrade_printing_preferences, upgrade_printing_preferences_thank_you}

class UpgradePrintingPreferencesViewSpec extends UnitSpec with WithFakeApplication {
  override lazy val fakeApplication = ConfigHelper.fakeApp

  "upgrade printing preferences template" should {
    "render the correct content" in {
      val emailAddress = "test@test.com"
      val returnUrl = Encrypted("someReturnUrl")
      val upgradeUrl = routes.UpgradeRemindersController.submitUpgrade(returnUrl).toString()

      val document = Jsoup.parse(upgrade_printing_preferences(Some(emailAddress), returnUrl, UpgradeRemindersForm())(FakeRequest("GET", "/")).toString())
      document.getElementById("opted-in-email").text() should include (emailAddress)
      document.getElementsByTag("form").attr("action") should be (upgradeUrl)
    }
  }

  "upgrade printing preferences thank you template" should {
    "render the correct content" in {
      val returnUrl = "anyOldUrl"

      val document = Jsoup.parse(upgrade_printing_preferences_thank_you(returnUrl)(FakeRequest("GET", "/")).toString())
      document.getElementsByClass("button").attr("href") should be(returnUrl)
    }
  }
}
