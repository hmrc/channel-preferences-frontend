package controllers.sa.prefs.partial

import connectors.{SaEmailPreference, SaPreference}
import org.joda.time.LocalDate
import play.api.mvc.Results
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.test.UnitSpec

class RenderViewForPreferencesSpec extends UnitSpec with Results with WithFakeApplication{

  val preferencesWarningView = new RenderViewForPreferences {}

  "rendering of preferences warnings" should {
    "have no content when opted out " in {
      preferencesWarningView.renderPrefs(Some(SaPreference(digital = false))) should be (None)
    }

    "have no content when verified" in {
      preferencesWarningView.renderPrefs(Some(SaPreference(digital = true, email = Some(SaEmailPreference(
        email = "test@test.com",
        status = SaEmailPreference.Status.verified))
      ))) should be (None)
    }

    "have pending verification warning if email not verified" in {
      val result = preferencesWarningView.renderPrefs(Some(SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.pending,
          linkSent = Some(LocalDate.parse("2014-12-05"))))
      )))
      val contentAsString = result.get.toString()
      contentAsString should include ("test@test.com")
      contentAsString should include (" 5 December 2014")
    }

    "have mail box full warning if email bounces due to mail box being full" in {
      val result = preferencesWarningView.renderPrefs(Some(SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.bounced,
          linkSent = Some(LocalDate.parse("2014-12-05")),
          mailboxFull = true))
      )))
      val contentAsString = result.get.toString()
      contentAsString should not include "test@test.com"
      contentAsString should not include "5 December 2014"
      contentAsString should include ("can't be sent because your inbox is full")
    }

    "have problem warning if email bounces due to some other reason than mail box being full" in {
      val result = preferencesWarningView.renderPrefs(Some(SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.bounced,
          linkSent = Some(LocalDate.parse("2014-12-05")),
          mailboxFull = false))
      )))
      val contentAsString = result.get.toString()
      contentAsString should not include "test@test.com"
      contentAsString should not include "5 December 2014"
      contentAsString should not include "inbox is full"
      contentAsString should include ("can't be delivered")
    }
  }
}
