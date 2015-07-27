package views.sa.prefs

import _root_.helpers.ConfigHelper
import controllers.sa.prefs.internal._
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.emailaddress.ObfuscatedEmailAddress
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import views.html.sa.prefs.upgrade_printing_preferences

class UpgradePrintingPreferencesViewSpec extends UnitSpec with PreferencesControllerHelper with WithFakeApplication {
  override lazy val fakeApplication = ConfigHelper.fakeApp

  "preference upgrade printing preferences template" should {
    "render the correct content" in {
      val emailAddress = "test@test.com"
      val returnUrl = "someReturnUrl"
      val upgradeUrl = routes.UpgradeRemindersController.upgrade(returnUrl).toString()

      val document = Jsoup.parse(upgrade_printing_preferences(Some(emailAddress), returnUrl, upgradeRemindersForm)(FakeRequest("GET", "/")).toString())
      document.getElementById("opted-in-email").text() should include (emailAddress)
      document.getElementsByTag("form").attr("action") should be (upgradeUrl)
    }
  }

  "submit the opt-in form" should {

  }

}
