package partial.paperless.manage

import _root_.helpers.{ConfigHelper, TestFixtures, WelshLanguage}
import connectors.EmailPreference
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import uk.gov.hmrc.play.test.UnitSpec
import html.digital_true_bounced
import play.api.test.FakeRequest

class DigitalTrueBouncedSpec extends UnitSpec with OneAppPerSuite with WelshLanguage {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "digital true bounced partial" should {
    "render the correct content in english when the mailbox is not full" in {
      val emailAddress = "a@a.com"
      val email = EmailPreference(emailAddress, true, true, false, None)
      val document = Jsoup.parse(digital_true_bounced(email)(FakeRequest(), applicationMessages, TestFixtures.sampleHostContext).toString())

      document.getElementById("bouncing-status-message").text() shouldBe s"You need to verify $emailAddress"
      document.getElementById("bounce-reason").text() shouldBe "The email telling you how to do this can't be delivered."
      document.getElementsByTag("p").get(2).text() shouldBe "Use a different email address"
    }

    "render the correct content in english when the mailbox is full" in {
      val emailAddress = "b@b.com"
      val email = EmailPreference(emailAddress, true, true, true, None)
      val document = Jsoup.parse(digital_true_bounced(email)(FakeRequest(), applicationMessages, TestFixtures.sampleHostContext).toString())

      document.getElementById("bounce-reason").text() shouldBe "The email telling you how to do this can't be sent because your inbox is full."
      document.getElementById("bounce-reason-more").text() shouldBe "Clear your inbox or use a different email address."
    }

    "render the correct content in welsh when the mailbox is not full" in {
      val emailAddress = "a@a.com"
      val email = EmailPreference(emailAddress, true, true, false, None)
      val document = Jsoup.parse(digital_true_bounced(email)(welshRequest, messagesInWelsh(applicationMessages), TestFixtures.sampleHostContext).toString())

      document.getElementById("bouncing-status-message").text() shouldBe s"Mae angen i chi ddilysu $emailAddress"
      document.getElementById("bounce-reason").text() shouldBe "Ni all yr e-bost sy'n rhoi gwybod i chi sut i wneud hyn gyrraedd pen ei daith."
      document.getElementsByTag("p").get(2).text() shouldBe "Defnyddiwch gyfeiriad e-bost gwahanol"
    }

    "render the correct content in welsh when the mailbox is full" in {
      val emailAddress = "b@b.com"
      val email = EmailPreference(emailAddress, true, true, true, None)
      val document = Jsoup.parse(digital_true_bounced(email)(welshRequest, messagesInWelsh(applicationMessages), TestFixtures.sampleHostContext).toString())

      document.getElementById("bounce-reason").text() shouldBe "Ni ellir anfon yr e-bost sy'n rhoi gwybod i chi sut i wneud hyn oherwydd bod eich mewnflwch yn llawn."
      document.getElementById("bounce-reason-more").text() shouldBe "Cliriwch eich mewnflwch neu defnyddiwch gyfeiriad e-bost gwahanol."
    }
  }
}