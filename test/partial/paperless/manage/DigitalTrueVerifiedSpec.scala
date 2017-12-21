package partial.paperless.manage

import _root_.helpers.{ConfigHelper, TestFixtures, WelshLanguage}
import connectors.EmailPreference
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import uk.gov.hmrc.play.test.UnitSpec
import html.digital_true_verified
import play.api.test.FakeRequest

class DigitalTrueVerifiedSpec extends UnitSpec with OneAppPerSuite with WelshLanguage {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "digital true verified partial" should {
    "render the correct content in english" in {
      val emailAddress = "a@a.com"
      val email = EmailPreference(emailAddress, true, true, false, None)
      val document = Jsoup.parse(digital_true_verified(email)(FakeRequest(), applicationMessages, TestFixtures.sampleHostContext, langEn).toString())

      document.getElementById("saEmailRemindersHeader").text() shouldBe "Email address for HMRC digital communications"
      document.getElementsByTag("p").get(0).childNodes().get(0).toString() shouldBe "Emails are sent to: "
    }

    "render the correct content in welsh" in {
      val emailAddress = "a@a.com"
      val email = EmailPreference(emailAddress, true, true, false, None)
      val document = Jsoup.parse(digital_true_verified(email)(welshRequest, messagesInWelsh(applicationMessages), TestFixtures.sampleHostContext, langCy).toString())

      document.getElementById("saEmailRemindersHeader").text() shouldBe "Cyfeiriad e-bost ar gyfer cyfathrebu'n ddigidol â CThEM"
      document.getElementsByTag("p").get(0).childNodes().get(0).toString() shouldBe "Anfonir e-byst at: "
    }
  }
}