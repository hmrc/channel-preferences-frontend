package partial.paperless.manage

import _root_.helpers.{ConfigHelper, TestFixtures}
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import uk.gov.hmrc.play.test.UnitSpec
import html.digital_false

class DigitalFalseSpec extends UnitSpec with OneAppPerSuite {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "digital false partial" should {
    "render the correct content in english" in {
      val document = Jsoup.parse(digital_false(None)(TestFixtures.sampleHostContext, applicationMessages).toString())

      document.getElementById("saEmailRemindersHeader").text() shouldBe "Go paperless"
      document.getElementById("opt-out-status-message").text() shouldBe "Replace the letters you get about taxes with emails."
      document.getElementById("opt-in-to-digital-email-link").text() shouldBe "Sign up for paperless notifications"
    }
  }
}