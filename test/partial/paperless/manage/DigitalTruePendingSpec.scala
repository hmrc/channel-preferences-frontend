package partial.paperless.manage

import _root_.helpers.{ConfigHelper, TestFixtures, WelshLanguage}
import connectors.EmailPreference
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import uk.gov.hmrc.play.test.UnitSpec
import html.digital_true_pending
import org.joda.time.LocalDate
import play.api.test.FakeRequest
import views.sa.prefs.helpers.DateFormat

class DigitalTruePendingSpec extends UnitSpec with OneAppPerSuite with WelshLanguage {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "digital true pending partial" should {
    "render the correct content in english" in {
      val emailAddress = "a@a.com"
      val localDate = new LocalDate("2017-12-01")
      val formattedLocalDate = DateFormat.longDateFormat(Some(localDate))
      val email = EmailPreference(emailAddress, true, true, false, Some(localDate))
      val document = Jsoup.parse(digital_true_pending(email)(FakeRequest(), applicationMessages, TestFixtures.sampleHostContext).toString())

      document.getElementById("saEmailRemindersHeader").text() shouldBe "Email for paperless notifications"
      document.getElementsByTag("p").get(0).text() shouldBe "You need to verify your email address."
      document.getElementById("pending-status-message").childNodes().get(0).toString() shouldBe "An email was sent to "
      document.getElementById("pending-status-message").childNodes().get(1).childNodes().get(0).toString() shouldBe emailAddress
      document.getElementById("pending-status-message").childNodes().get(2).toString() shouldBe s" on ${formattedLocalDate.get}. Click on the link in the email to verify your email address."
      document.getElementsByTag("p").get(2).childNodes().get(0).toString() shouldBe " If you can't find it, we can "
      document.getElementById("resend-email-button").text() shouldBe "send a new verification email"
    }

    "render the correct content in welsh" in {
      val emailAddress = "a@a.com"
      val localDate = new LocalDate("2017-12-01")
      val formattedLocalDate = DateFormat.longDateFormat(Some(localDate))
      val email = EmailPreference(emailAddress, true, true, false, Some(localDate))
      val document = Jsoup.parse(digital_true_pending(email)(welshRequest, messagesInWelsh(applicationMessages), TestFixtures.sampleHostContext).toString())

      document.getElementById("saEmailRemindersHeader").text() shouldBe "E-bost ar gyfer hysbysiadau di-bapur"
      document.getElementsByTag("p").get(0).text() shouldBe "Mae angen i chi ddilysuch cyfeiriad e-bost."
      document.getElementById("pending-status-message").childNodes().get(0).toString() shouldBe "Anfonwyd e-bost at "
      document.getElementById("pending-status-message").childNodes().get(1).childNodes().get(0).toString() shouldBe emailAddress
      document.getElementById("pending-status-message").childNodes().get(2).toString() shouldBe s" ar ${formattedLocalDate.get}. Cliciwch ar y cysylltiad yn yr e-bost er mwyn dilysu'ch cyfeiriad e-bost."
      document.getElementsByTag("p").get(2).childNodes().get(0).toString() shouldBe " Os na allwch ddod o hyd iddo, "
      document.getElementById("resend-email-button").text() shouldBe "gallwch gael e-bost newydd wedi'i anfon atoch o"
    }
  }
}