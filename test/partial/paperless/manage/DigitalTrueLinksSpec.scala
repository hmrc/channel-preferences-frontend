package partial.paperless.manage

import _root_.helpers.{ConfigHelper, TestFixtures, WelshLanguage}
import connectors.EmailPreference
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import uk.gov.hmrc.play.test.UnitSpec
import html.digital_true_links
import play.api.test.FakeRequest

class DigitalTrueLinksSpec extends UnitSpec with OneAppPerSuite with WelshLanguage {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "digital true links partial" should {
    "render the correct content in english" in {
      val emailAddress = "a@a.com"
      val linkId = "test_id"
      val email = EmailPreference(emailAddress, true, true, false, None)
      val document = Jsoup.parse(digital_true_links(email, linkId)(FakeRequest(), applicationMessages, TestFixtures.sampleHostContext, langEn).toString())

      document.getElementById(linkId).text() shouldBe "Change your email address"
      document.getElementById("opt-out-of-email-link").text() shouldBe "Stop emails from HMRC"
    }

    "render the correct content in welsh" in {
      val emailAddress = "a@a.com"
      val linkId = "test_id"
      val email = EmailPreference(emailAddress, true, true, false, None)
      val document = Jsoup.parse(digital_true_links(email, linkId)(welshRequest, messagesInWelsh(applicationMessages), TestFixtures.sampleHostContext, langCy).toString())

      document.getElementById(linkId).text() shouldBe "Newid eich cyfeiriad e-bost"
      document.getElementById("opt-out-of-email-link").text() shouldBe "Atal e-byst gan CThEM"
    }
  }
}