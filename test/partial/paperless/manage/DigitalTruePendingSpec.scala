/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package partial.paperless.manage

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import connectors.EmailPreference
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import org.scalatestplus.play.PlaySpec
import html.digital_true_pending
import org.joda.time.LocalDate
import play.api.test.FakeRequest
import views.sa.prefs.helpers.DateFormat

class DigitalTruePendingSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[digital_true_pending]

  "digital true pending partial" should {
    "render the correct content in english" in {
      val emailAddress = "a@a.com"
      val localDate = new LocalDate("2017-12-01")
      val formattedLocalDate = DateFormat.longDateFormat(Some(localDate))
      val email = EmailPreference(emailAddress, true, true, false, Some(localDate))
      val document =
        Jsoup.parse(template(email)(FakeRequest(), messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      document.getElementById("saEmailRemindersHeader").text() mustBe "Email for paperless notifications"
      document.getElementsByTag("p").get(0).text() mustBe "You need to verify your email address."
      document.getElementById("pending-status-message").childNodes().get(0).toString() mustBe "An email was sent to "
      document
        .getElementById("pending-status-message")
        .childNodes()
        .get(1)
        .childNodes()
        .get(0)
        .toString() mustBe emailAddress
      document
        .getElementById("pending-status-message")
        .childNodes()
        .get(2)
        .toString() mustBe s" on ${formattedLocalDate.get}. Click on the link in the email to verify your email address."
      document.getElementsByTag("p").get(2).childNodes().get(0).toString() mustBe " If you can't find it, we can "
      document.getElementById("resend-email-button").text() mustBe "Send a new verification email"
    }

    "render the correct content in welsh" in {
      val emailAddress = "a@a.com"
      val localDate = new LocalDate("2017-12-01")
      val formattedLocalDate = DateFormat.longDateFormat(Some(localDate))
      val email = EmailPreference(emailAddress, true, true, false, Some(localDate))
      val document =
        Jsoup.parse(template(email)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document.getElementById("saEmailRemindersHeader").text() mustBe "E-bost ar gyfer hysbysiadau di-bapur"
      document.getElementsByTag("p").get(0).text() mustBe "Mae angen i chi ddilysuch cyfeiriad e-bost."
      document.getElementById("pending-status-message").childNodes().get(0).toString() mustBe "Anfonwyd e-bost at "
      document
        .getElementById("pending-status-message")
        .childNodes()
        .get(1)
        .childNodes()
        .get(0)
        .toString() mustBe emailAddress
      document
        .getElementById("pending-status-message")
        .childNodes()
        .get(2)
        .toString() mustBe s" ar ${formattedLocalDate.get}. Cliciwch ar y cysylltiad yn yr e-bost er mwyn dilysu'ch cyfeiriad e-bost."
      document.getElementsByTag("p").get(2).childNodes().get(0).toString() mustBe " Os na allwch ddod o hyd iddo, "
      document.getElementById("resend-email-button").text() mustBe "Gallwch gael e-bost newydd wedi'i anfon atoch o"
    }
  }
}
