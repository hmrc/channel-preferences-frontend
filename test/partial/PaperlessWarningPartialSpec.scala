package partial

import connectors.{SaEmailPreference, SaPreference}
import helpers.ConfigHelper
import org.joda.time.LocalDate
import partial.paperless.warnings.PaperlessWarningPartial
import play.api.mvc.Results
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class PaperlessWarningPartialSpec extends UnitSpec with Results with WithFakeApplication {
  override lazy val fakeApplication = ConfigHelper.fakeApp
  "rendering of preferences warnings" should {
    "have no content when opted out " in {
      PaperlessWarningPartial.apply(SaPreference(digital = false), "unused.url.com", "Unused text").body should be("")
    }

    "have no content when verified" in {
      PaperlessWarningPartial.apply(SaPreference(digital = true, email = Some(SaEmailPreference(
        email = "test@test.com",
        status = SaEmailPreference.Status.Verified))
      ), "unused.url.com", "Unused Text").body should be("")
    }

    "have pending verification warning if email not verified" in {
      val result = PaperlessWarningPartial.apply(SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.Pending,
          linkSent = Some(LocalDate.parse("2014-12-05"))))
      ), "manage.account.com", "Manage account").body
      result should include ("test@test.com")
      result should include ("5 December 2014")
      result should include ("Manage account")
      result should include ("manage.account.com")
    }

    "have mail box full warning if email bounces due to mail box being full" in {
      val result = PaperlessWarningPartial.apply(SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.Bounced,
          linkSent = Some(LocalDate.parse("2014-12-05")),
          mailboxFull = true))
      ), "unused.url.com", "Unused Text").body
      result should include ("Your inbox is full")
    }

    "have problem warning if email bounces due to some other reason than mail box being full" in {
      val result = PaperlessWarningPartial.apply(SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.Bounced,
          linkSent = Some(LocalDate.parse("2014-12-05")),
          mailboxFull = false))
      ), "manage.account.com", "Manage account").body
      result should include ("There's a problem with your paperless notification emails")
      result should include ("Manage account")
      result should include ("manage.account.com")
    }
  }
}
