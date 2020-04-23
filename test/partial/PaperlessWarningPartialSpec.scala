/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package partial

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
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
import org.jsoup.Jsoup

class PaperlessWarningPartialSpec
    extends PlaySpec with Results with GuiceOneAppPerSuite with LanguageHelper with MetricOrchestratorStub
    with ConfigHelper {

  override implicit lazy val app: Application = fakeApp

  val appMessages = messagesInEnglish()
  "rendering of preferences warnings" must {
    "have no content when opted out " in {
      PaperlessWarningPartial
        .apply(SaPreference(digital = false).toNewPreference(), TestFixtures.sampleHostContext)(
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
          TestFixtures.sampleHostContext
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
          TestFixtures.sampleHostContext
        )(FakeRequest(), appMessages)
        .body

      val document = Jsoup.parse(result)
      result must include("test@test.com")
      result must include("5 December 2014")
      document.getElementById("remindersProblem").text() mustBe "Continue"
      document
        .getElementById("remindersProblem")
        .attr("href") mustBe "/paperless/check-settings?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D"
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
        PaperlessWarningPartial.apply(saPref, TestFixtures.sampleHostContext)(FakeRequest(), appMessages).body
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
          TestFixtures.sampleHostContext
        )(FakeRequest(), appMessages)
        .body
      val document = Jsoup.parse(result)
      result must include("There&#x27;s a problem with your paperless notification emails")
      document.getElementById("remindersProblem").text() mustBe "Continue"
      document
        .getElementById("remindersProblem")
        .attr("href") mustBe "/paperless/check-settings?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D"
    }

    "be ignored if the preferences is not opted in for generic for not verified emails" in new TestCase {
      PaperlessWarningPartial.apply(
        genericOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = false),
        TestFixtures.sampleHostContext)(FakeRequest(), appMessages) must not be HtmlFormat.empty
      PaperlessWarningPartial.apply(
        genericOnlyPreferenceResponse(accepted = false, isVerified = false, hasBounces = false),
        TestFixtures.sampleHostContext)(FakeRequest(), appMessages) mustBe HtmlFormat.empty
      PaperlessWarningPartial.apply(
        taxCreditOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = false),
        TestFixtures.sampleHostContext)(FakeRequest(), appMessages) mustBe HtmlFormat.empty
      PaperlessWarningPartial.apply(
        taxCreditOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = false),
        TestFixtures.sampleHostContext)(FakeRequest(), appMessages) mustBe HtmlFormat.empty
    }

    "be ignored if the preferences is not opted in for generic for bounced emails" in new TestCase {
      PaperlessWarningPartial.apply(
        genericOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = true),
        TestFixtures.sampleHostContext)(FakeRequest(), appMessages) must not be HtmlFormat.empty
      PaperlessWarningPartial.apply(
        genericOnlyPreferenceResponse(accepted = false, isVerified = false, hasBounces = true),
        TestFixtures.sampleHostContext)(FakeRequest(), appMessages) mustBe HtmlFormat.empty
      PaperlessWarningPartial.apply(
        taxCreditOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = true),
        TestFixtures.sampleHostContext)(FakeRequest(), appMessages) mustBe HtmlFormat.empty
      PaperlessWarningPartial.apply(
        taxCreditOnlyPreferenceResponse(accepted = true, isVerified = false, hasBounces = true),
        TestFixtures.sampleHostContext)(FakeRequest(), appMessages) mustBe HtmlFormat.empty
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
          TestFixtures.sampleHostContext
        )(welshRequest, messagesInWelsh())
        .body

      val document = Jsoup.parse(result)
      result must include("test@test.com")
      result must include("5 December 2014")
      document.getElementById("remindersProblem").text() mustBe "Yn eich blaen"
      document
        .getElementById("remindersProblem")
        .attr("href") mustBe "/paperless/check-settings?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D"

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
        .apply(saPref, TestFixtures.sampleHostContext)(welshRequest, messagesInWelsh())
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
          TestFixtures.sampleHostContext
        )(welshRequest, messagesInWelsh())
        .body
      val document = Jsoup.parse(result)
      result must include("Mae yna broblem gyda&#x27;ch e-byst hysbysu di-bapur")
      document.getElementById("remindersProblem").text() mustBe "Yn eich blaen"
      document
        .getElementById("remindersProblem")
        .attr("href") mustBe "/paperless/check-settings?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D"

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
               |    "status": "verified",
               |    "language": "en"
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
               |    "status": "verified",
               |    "language": "en"
               |  },
               |  "digital": true
               |}""".stripMargin
          )
          .as[PreferenceResponse]
    }

  }
}
