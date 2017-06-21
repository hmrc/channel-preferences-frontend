package partial

import connectors.{PreferenceResponse, SaEmailPreference, SaPreference}
import helpers.ConfigHelper
import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import partial.paperless.warnings.PaperlessWarningPartial
import play.api.Application
import play.api.mvc.Results
import uk.gov.hmrc.play.test.UnitSpec
import connectors.PreferenceResponse._
import play.api.libs.json.Json
import play.twirl.api.HtmlFormat


class PaperlessWarningPartialSpec extends UnitSpec with Results with OneAppPerSuite {

  override implicit lazy val app : Application = ConfigHelper.fakeApp

  "rendering of preferences warnings" should {
    "have no content when opted out " in {
      PaperlessWarningPartial.apply(SaPreference(digital = false).toNewPreference(), "unused.url.com", "Unused text").body should be("")
    }

    "have no content when verified" in {
      PaperlessWarningPartial.apply(SaPreference(digital = true, email = Some(SaEmailPreference(
        email = "test@test.com",
        status = SaEmailPreference.Status.Verified))
      ).toNewPreference, "unused.url.com", "Unused Text").body should be("")
    }

    "have pending verification warning if email not verified" in {
      val result = PaperlessWarningPartial.apply(SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.Pending,
          linkSent = Some(LocalDate.parse("2014-12-05"))))
      ).toNewPreference(), "manage.account.com", "Manage account").body
      result should include ("test@test.com")
      result should include ("5 December 2014")
      result should include ("Manage account")
      result should include ("manage.account.com")
    }

    "have mail box full warning if email bounces due to mail box being full" in {
      val saPref = SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.Bounced,
          linkSent = Some(LocalDate.parse("2014-12-05")),
          mailboxFull = true))
      ).toNewPreference()
      val result = PaperlessWarningPartial.apply(saPref, "unused.url.com", "Unused Text").body
      result should include ("Your inbox is full")
    }

    "have problem warning if email bounces due to some other reason than mail box being full" in {
      val result = PaperlessWarningPartial.apply(SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.Bounced,
          linkSent = Some(LocalDate.parse("2014-12-05")),
          mailboxFull = false))
      ).toNewPreference(), "manage.account.com", "Manage account").body
      result should include ("There's a problem with your paperless notification emails")
      result should include ("Manage account")
      result should include ("manage.account.com")
    }

    "be ignored if the preferences is not opted in for generic for not verified emails" in new TestCase {
      PaperlessWarningPartial.apply(genericOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = false), "", "") should not be HtmlFormat.empty
      PaperlessWarningPartial.apply(genericOnlyPreferenceResponse(accepted = false, isVerified = false, hasBounces = false), "", "") shouldBe HtmlFormat.empty
      PaperlessWarningPartial.apply(taxCreditOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = false), "", "") shouldBe HtmlFormat.empty
      PaperlessWarningPartial.apply(taxCreditOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = false), "", "") shouldBe HtmlFormat.empty
    }

    "be ignored if the preferences is not opted in for generic for bounced emails" in new TestCase {
      PaperlessWarningPartial.apply(genericOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = true), "", "") should not be HtmlFormat.empty
      PaperlessWarningPartial.apply(genericOnlyPreferenceResponse(accepted = false, isVerified = false, hasBounces = true), "", "") shouldBe HtmlFormat.empty
      PaperlessWarningPartial.apply(taxCreditOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = true), "", "") shouldBe HtmlFormat.empty
      PaperlessWarningPartial.apply(taxCreditOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = true), "", "") shouldBe HtmlFormat.empty
    }

    trait TestCase {
      def taxCreditOnlyPreferenceResponse(accepted: Boolean, isVerified : Boolean, hasBounces: Boolean) = Json.parse(
        s"""{
           |  "termsAndConditions": {
           |    "taxCredits": {
           |      "accepted": $accepted
           |    }
           |  },
           |  "email": {
           |    "email": "pihklyljtgoxeoh@mail.com",
           |    "isVerified": $isVerified,
           |    "hasBounces": $hasBounces,
           |    "mailboxFull": false,
           |    "status": "verified"
           |  },
           |  "digital": true
           |}""".stripMargin
      ).as[PreferenceResponse]

      def genericOnlyPreferenceResponse(accepted: Boolean, isVerified : Boolean, hasBounces: Boolean) = Json.parse(
        s"""{
           |  "termsAndConditions": {
           |    "generic": {
           |      "accepted": $accepted
           |    }
           |  },
           |  "email": {
           |    "email": "pihklyljtgoxeoh@mail.com",
           |    "isVerified": $isVerified,
           |    "hasBounces": $hasBounces,
           |    "mailboxFull": false,
           |    "status": "verified"
           |  },
           |  "digital": true
           |}""".stripMargin
      ).as[PreferenceResponse]
    }

  }
}
