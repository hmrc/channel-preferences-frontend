package partial

import connectors.{PreferenceResponse, SaEmailPreference, SaPreference}
import helpers.ConfigHelper
import _root_.helpers.WelshLanguage
import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import partial.paperless.warnings.PaperlessWarningPartial
import play.api.Application
import play.api.mvc.Results
import uk.gov.hmrc.play.test.UnitSpec
import connectors.PreferenceResponse._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import play.api.i18n.Messages.Implicits._

class PaperlessWarningPartialSpec extends UnitSpec with Results with OneAppPerSuite with WelshLanguage {

  override implicit lazy val app : Application = ConfigHelper.fakeApp

  "rendering of preferences warnings" should {
    "have no content when opted out " in {
      PaperlessWarningPartial.apply(SaPreference(digital = false).toNewPreference(), "unused.url.com", "Unused text")(FakeRequest(), applicationMessages).body should be("")
    }

    "have no content when verified" in {
      PaperlessWarningPartial.apply(SaPreference(digital = true, email = Some(SaEmailPreference(
        email = "test@test.com",
        status = SaEmailPreference.Status.Verified))
      ).toNewPreference, "unused.url.com", "Unused Text")(FakeRequest(), applicationMessages).body should be("")
    }

    "have pending verification warning in english if email not verified" in {
      val result = PaperlessWarningPartial.apply(SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.Pending,
          linkSent = Some(LocalDate.parse("2014-12-05"))))
      ).toNewPreference(), "manage.account.com", "Manage account")(FakeRequest(), applicationMessages).body
      result should include ("test@test.com")
      result should include ("5 December 2014")
      result should include ("Manage account")
      result should include ("manage.account.com")
    }

    "have mail box full warning in english if email bounces due to mail box being full" in {
      val saPref = SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.Bounced,
          linkSent = Some(LocalDate.parse("2014-12-05")),
          mailboxFull = true))
      ).toNewPreference()
      val result = PaperlessWarningPartial.apply(saPref, "unused.url.com", "Unused Text")(FakeRequest(), applicationMessages).body
      result should include ("Your inbox is full")
    }

    "have problem warning in english if email bounces due to some other reason than mail box being full" in {
      val result = PaperlessWarningPartial.apply(SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.Bounced,
          linkSent = Some(LocalDate.parse("2014-12-05")),
          mailboxFull = false))
      ).toNewPreference(), "manage.account.com", "Manage account")(FakeRequest(), applicationMessages).body
      result should include ("There&#x27;s a problem with your paperless notification emails")
      result should include ("Manage account")
      result should include ("manage.account.com")
    }

    "be ignored if the preferences is not opted in for generic for not verified emails" in new TestCase {
      PaperlessWarningPartial.apply(genericOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = false), "", "")(FakeRequest(), applicationMessages) should not be HtmlFormat.empty
      PaperlessWarningPartial.apply(genericOnlyPreferenceResponse(accepted = false, isVerified = false, hasBounces = false), "", "")(FakeRequest(), applicationMessages) shouldBe HtmlFormat.empty
      PaperlessWarningPartial.apply(taxCreditOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = false), "", "")(FakeRequest(), applicationMessages) shouldBe HtmlFormat.empty
      PaperlessWarningPartial.apply(taxCreditOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = false), "", "")(FakeRequest(), applicationMessages) shouldBe HtmlFormat.empty
    }

    "be ignored if the preferences is not opted in for generic for bounced emails" in new TestCase {
      PaperlessWarningPartial.apply(genericOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = true), "", "")(FakeRequest(), applicationMessages) should not be HtmlFormat.empty
      PaperlessWarningPartial.apply(genericOnlyPreferenceResponse(accepted = false, isVerified = false, hasBounces = true), "", "")(FakeRequest(), applicationMessages) shouldBe HtmlFormat.empty
      PaperlessWarningPartial.apply(taxCreditOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = true), "", "")(FakeRequest(), applicationMessages) shouldBe HtmlFormat.empty
      PaperlessWarningPartial.apply(taxCreditOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = true), "", "")(FakeRequest(), applicationMessages) shouldBe HtmlFormat.empty
    }

    "have pending verification warning in welsh if email not verified" in {
      implicit val messages = messagesInWelsh(applicationMessages)
      val result = PaperlessWarningPartial.apply(SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.Pending,
          linkSent = Some(LocalDate.parse("2014-12-05"))))
      ).toNewPreference(), "manage.account.com", "Manage account")(welshRequest, messagesInWelsh(applicationMessages)).body
      result should include ("test@test.com")
      result should include ("5 December 2014")
      result should include ("Rheoli'r cyfrif")
      result should include ("manage.account.com WELSH")
    }

    "have mail box full warning in welsh if email bounces due to mail box being full" in {
      val saPref = SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.Bounced,
          linkSent = Some(LocalDate.parse("2014-12-05")),
          mailboxFull = true))
      ).toNewPreference()
      val result = PaperlessWarningPartial.apply(saPref, "unused.url.com", "Unused Text")(welshRequest, messagesInWelsh(applicationMessages)).body
      result should include ("Mae&#x27;ch mewnflwch yn llawn.")
    }

    "have problem warning in welsh if email bounces due to some other reason than mail box being full" in {
      val result = PaperlessWarningPartial.apply(SaPreference(
        digital = true, email = Some(SaEmailPreference(
          email = "test@test.com",
          status = SaEmailPreference.Status.Bounced,
          linkSent = Some(LocalDate.parse("2014-12-05")),
          mailboxFull = false))
      ).toNewPreference(), "manage.account.com", "Manage account")(FakeRequest(), applicationMessages).body
      result should include ("There&#x27;s a problem with your paperless notification emails WELSH")
      result should include ("Manage account WELSH")
      result should include ("manage.account.com WELSH")
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
