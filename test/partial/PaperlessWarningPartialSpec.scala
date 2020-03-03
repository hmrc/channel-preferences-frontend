/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package partial

import _root_.helpers.{ ConfigHelper, LanguageHelper }
import connectors.PreferenceResponse._
import connectors.{ PreferenceResponse, SaEmailPreference, SaPreference }
import controllers.MetricOrchestratorStub
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import partial.paperless.warnings.PaperlessWarningPartial
import play.api.Application
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat

class PaperlessWarningPartialSpec
    extends PlaySpec with Results with GuiceOneAppPerSuite with LanguageHelper with MetricOrchestratorStub
    with ConfigHelper {

  override implicit lazy val app: Application = fakeApp

  val appMessages = messagesInEnglish()
  "rendering of preferences warnings" must {
    "have no content when opted out " in {
      PaperlessWarningPartial
        .apply(SaPreference(digital = false).toNewPreference(), "unused.url.com", "Unused text")(
          FakeRequest(),
          appMessages)
        .body must be("")
    }

    "have no content when verified" in {
      PaperlessWarningPartial
        .apply(
          SaPreference(
            digital = true,
            email = Some(SaEmailPreference(email = "test@test.com", status = SaEmailPreference.Status.Verified))).toNewPreference,
          "unused.url.com",
          "Unused Text"
        )(FakeRequest(), appMessages)
        .body must be("")
    }

    "have pending verification warning in english if email not verified" in {
      val result = PaperlessWarningPartial
        .apply(
          SaPreference(
            digital = true,
            email = Some(
              SaEmailPreference(
                email = "test@test.com",
                status = SaEmailPreference.Status.Pending,
                linkSent = Some(LocalDate.parse("2014-12-05"))))
          ).toNewPreference(),
          "manage.account.com",
          "Manage account"
        )(FakeRequest(), appMessages)
        .body
      result must include("test@test.com")
      result must include("5 December 2014")
      result must include("Manage account")
      result must include("manage.account.com")
    }

    "have mail box full warning in english if email bounces due to mail box being full" in {
      val saPref = SaPreference(
        digital = true,
        email = Some(
          SaEmailPreference(
            email = "test@test.com",
            status = SaEmailPreference.Status.Bounced,
            linkSent = Some(LocalDate.parse("2014-12-05")),
            mailboxFull = true))
      ).toNewPreference()
      val result =
        PaperlessWarningPartial.apply(saPref, "unused.url.com", "Unused Text")(FakeRequest(), appMessages).body
      result must include("Your inbox is full")
    }

    "have problem warning in english if email bounces due to some other reason than mail box being full" in {
      val result = PaperlessWarningPartial
        .apply(
          SaPreference(
            digital = true,
            email = Some(
              SaEmailPreference(
                email = "test@test.com",
                status = SaEmailPreference.Status.Bounced,
                linkSent = Some(LocalDate.parse("2014-12-05")),
                mailboxFull = false))
          ).toNewPreference(),
          "manage.account.com",
          "Manage account"
        )(FakeRequest(), appMessages)
        .body
      result must include("There&#x27;s a problem with your paperless notification emails")
      result must include("Manage account")
      result must include("manage.account.com")
    }

    "be ignored if the preferences is not opted in for generic for not verified emails" in new TestCase {
      PaperlessWarningPartial.apply(
        genericOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = false),
        "",
        "")(FakeRequest(), appMessages) must not be HtmlFormat.empty
      PaperlessWarningPartial.apply(
        genericOnlyPreferenceResponse(accepted = false, isVerified = false, hasBounces = false),
        "",
        "")(FakeRequest(), appMessages) mustBe HtmlFormat.empty
      PaperlessWarningPartial.apply(
        taxCreditOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = false),
        "",
        "")(FakeRequest(), appMessages) mustBe HtmlFormat.empty
      PaperlessWarningPartial.apply(
        taxCreditOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = false),
        "",
        "")(FakeRequest(), appMessages) mustBe HtmlFormat.empty
    }

    "be ignored if the preferences is not opted in for generic for bounced emails" in new TestCase {
      PaperlessWarningPartial.apply(
        genericOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = true),
        "",
        "")(FakeRequest(), appMessages) must not be HtmlFormat.empty
      PaperlessWarningPartial.apply(
        genericOnlyPreferenceResponse(accepted = false, isVerified = false, hasBounces = true),
        "",
        "")(FakeRequest(), appMessages) mustBe HtmlFormat.empty
      PaperlessWarningPartial.apply(
        taxCreditOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = true),
        "",
        "")(FakeRequest(), appMessages) mustBe HtmlFormat.empty
      PaperlessWarningPartial.apply(
        taxCreditOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = true),
        "",
        "")(FakeRequest(), appMessages) mustBe HtmlFormat.empty
    }

    "have pending verification warning in welsh if email not verified" in {
      implicit val messages = messagesInWelsh()
      val result = PaperlessWarningPartial
        .apply(
          SaPreference(
            digital = true,
            email = Some(
              SaEmailPreference(
                email = "test@test.com",
                status = SaEmailPreference.Status.Pending,
                linkSent = Some(LocalDate.parse("2014-12-05"))))
          ).toNewPreference(),
          "manage.account.com",
          "Manage account"
        )(welshRequest, messagesInWelsh())
        .body
      result must include("test@test.com")
      result must include("5 December 2014")
      result must include("Manage account")
      result must include("manage.account.com")
    }

    "have mail box full warning in welsh if email bounces due to mail box being full" in {
      val saPref = SaPreference(
        digital = true,
        email = Some(
          SaEmailPreference(
            email = "test@test.com",
            status = SaEmailPreference.Status.Bounced,
            linkSent = Some(LocalDate.parse("2014-12-05")),
            mailboxFull = true))
      ).toNewPreference()
      val result = PaperlessWarningPartial
        .apply(saPref, "unused.url.com", "Unused Text")(welshRequest, messagesInWelsh())
        .body
      result must include("Mae&#x27;ch mewnflwch yn llawn.")
    }

    "have problem warning in welsh if email bounces due to some other reason than mail box being full" in {
      val result = PaperlessWarningPartial
        .apply(
          SaPreference(
            digital = true,
            email = Some(
              SaEmailPreference(
                email = "test@test.com",
                status = SaEmailPreference.Status.Bounced,
                linkSent = Some(LocalDate.parse("2014-12-05")),
                mailboxFull = false))
          ).toNewPreference(),
          "manage.account.com",
          "Manage account"
        )(welshRequest, messagesInWelsh())
        .body
      result must include("Mae yna broblem gyda&#x27;ch e-byst hysbysu di-bapur")
      result must include("Manage account")
      result must include("manage.account.com")
    }

    trait TestCase {
      def taxCreditOnlyPreferenceResponse(accepted: Boolean, isVerified: Boolean, hasBounces: Boolean) =
        Json
          .parse(
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
          )
          .as[PreferenceResponse]

      def genericOnlyPreferenceResponse(accepted: Boolean, isVerified: Boolean, hasBounces: Boolean) =
        Json
          .parse(
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
          )
          .as[PreferenceResponse]
    }

  }
}
