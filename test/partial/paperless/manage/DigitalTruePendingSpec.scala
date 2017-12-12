package partial.paperless.manage

import _root_.helpers.{ConfigHelper, TestFixtures}
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

class DigitalTruePendingSpec extends UnitSpec with OneAppPerSuite {

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
  }
}