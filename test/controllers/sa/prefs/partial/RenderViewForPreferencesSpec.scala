package controllers.sa.prefs.partial

import connectors.{SaEmailPreference, SaPreference}
import controllers.sa.prefs.partial.homepage.RenderViewForPreferences
import helpers.ConfigHelper
import org.joda.time.LocalDate
import play.api.mvc.Results
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class RenderViewForPreferencesSpec extends UnitSpec with Results with WithFakeApplication {
  override lazy val fakeApplication = ConfigHelper.fakeApp
  val preferencesWarningView = new RenderViewForPreferences {}

  "rendering of preferences warnings" should {
    "have no content when opted out " in {
      preferencesWarningView.renderPrefs(SaPreference(digital = false)).body should be("")
    }

    "have no content when verified" in {
      preferencesWarningView.renderPrefs(SaPreference(digital = true, email = Some(SaEmailPreference(
        email = "test@test.com",
        status = SaEmailPreference.Status.verified))
      )).body should be("")
    }

    "have pending verification warning if email not verified" in {
      val result = preferencesWarningView.renderPrefs(SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.pending,
          linkSent = Some(LocalDate.parse("2014-12-05"))))
      )).body
      result should include ("test@test.com")
      result should include (" 5 December 2014")
    }

    "have mail box full warning if email bounces due to mail box being full" in {
      val result = preferencesWarningView.renderPrefs(SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.bounced,
          linkSent = Some(LocalDate.parse("2014-12-05")),
          mailboxFull = true))
      )).body
      result should include ("Your inbox is full")
    }

    "have problem warning if email bounces due to some other reason than mail box being full" in {
      val result = preferencesWarningView.renderPrefs(SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.bounced,
          linkSent = Some(LocalDate.parse("2014-12-05")),
          mailboxFull = false))
      )).body
      result should include ("There’s a problem with your Self Assessment email reminders")
    }
  }
}
