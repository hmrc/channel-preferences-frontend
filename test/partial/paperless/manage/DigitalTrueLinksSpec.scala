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
import html.digital_true_links
import play.api.test.FakeRequest

class DigitalTrueLinksSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[digital_true_links]

  "digital true links partial" should {
    "render the correct content in english" in {
      val emailAddress = "a@a.com"
      val linkId = "test_id"
      val email = EmailPreference(emailAddress, true, true, false, None)
      val document = Jsoup.parse(
        template(email, linkId)(FakeRequest(), messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      document.getElementById(linkId).text() mustBe "Change your email address"
      document.getElementById("opt-out-of-email-link").text() mustBe "Stop emails from HMRC"
    }

    "render the correct content in welsh" in {
      val emailAddress = "a@a.com"
      val linkId = "test_id"
      val email = EmailPreference(emailAddress, true, true, false, None)
      val document =
        Jsoup.parse(template(email, linkId)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document.getElementById(linkId).text() mustBe "Newid eich cyfeiriad e-bost"
      document.getElementById("opt-out-of-email-link").text() mustBe "Atal e-byst gan CThEM"
    }
  }
}
