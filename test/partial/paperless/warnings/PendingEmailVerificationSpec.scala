/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package partial.paperless.warnings

import _root_.helpers.{ ConfigHelper, LanguageHelper }
import connectors.EmailPreference
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import org.scalatestplus.play.PlaySpec
import html.pending_email_verification
import org.joda.time.LocalDate
import play.api.data.FormError
import play.api.test.FakeRequest
import views.sa.prefs.helpers.DateFormat

class PendingEmailVerificationSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp

  "pending email verification partial" should {
    "render the correct content in english" in {
      val emailAddress = "a@a.com"
      val localDate = new LocalDate("2017-12-01")
      val formattedLocalDate = DateFormat.longDateFormat(Some(localDate))
      val emailPreference = EmailPreference(emailAddress, true, true, false, Some(localDate))
      val errors = Seq((FormError("ErrorKey", Seq("Error Message"), Seq()), "Outer Error Message"))
      val document = Jsoup.parse(
        pending_email_verification(emailPreference, "returnUrl", "returnLinkText")(FakeRequest(), messagesInEnglish())
          .toString())

      document
        .getElementsByTag("summary")
        .first()
        .childNode(0)
        .toString() mustBe "Verify your email address for paperless notifications"
      document.getElementsByClass("flag--urgent").first().text() mustBe "Now"
      document
        .getElementsByTag("p")
        .get(0)
        .text() mustBe s"An email was sent to $emailAddress on ${formattedLocalDate.get}. Click on the link in the email to verify your email address with HMRC."
      document
        .getElementsByTag("p")
        .get(1)
        .childNode(0)
        .toString() mustBe "If you can't find it you can get a new email sent to you from "
    }

    "render the correct content in welsh" in {
      val emailAddress = "a@a.com"
      val localDate = new LocalDate("2017-12-01")
      val formattedLocalDate = DateFormat.longDateFormat(Some(localDate))
      val emailPreference = EmailPreference(emailAddress, true, true, false, Some(localDate))
      val errors = Seq((FormError("ErrorKey", Seq("Error Message"), Seq()), "Outer Error Message"))
      val document = Jsoup.parse(
        pending_email_verification(emailPreference, "returnUrl", "returnLinkText")(welshRequest, messagesInWelsh())
          .toString())

      document
        .getElementsByTag("summary")
        .first()
        .childNode(0)
        .toString() mustBe "Dilyswch eich cyfeiriad e-bost ar gyfer hysbysiadau di-bapur"
      document.getElementsByClass("flag--urgent").first().text() mustBe "Nawr"
      document
        .getElementsByTag("p")
        .get(0)
        .text() mustBe s"Anfonwyd e-bost at $emailAddress ar ${formattedLocalDate.get}. Cliciwch ar y cysylltiad yn yr e-bost er mwyn dilysu'ch cyfeiriad e-bost gyda CThEM."
      document
        .getElementsByTag("p")
        .get(1)
        .childNode(0)
        .toString() mustBe "Os na allwch ddod o hyd iddo, gallwch gael e-bost newydd wedi'i anfon atoch o "
    }
  }
}
