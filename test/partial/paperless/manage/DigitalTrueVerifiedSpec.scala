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
import html.digital_true_verified
import play.api.test.FakeRequest

class DigitalTrueVerifiedSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[digital_true_verified]

  "digital true verified partial" should {
    "render the correct content in english" in {
      val emailAddress = "a@a.com"
      val email = EmailPreference(emailAddress, true, true, false, None)
      val document =
        Jsoup.parse(template(email)(FakeRequest(), messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      document.getElementById("saEmailRemindersHeader").text() mustBe "Email address for HMRC digital communications"
      document.getElementsByTag("p").get(0).childNodes().get(0).toString() mustBe "Emails are sent to: "
    }

    "render the correct content in welsh" in {
      val emailAddress = "a@a.com"
      val email = EmailPreference(emailAddress, true, true, false, None)
      val document =
        Jsoup.parse(template(email)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document
        .getElementById("saEmailRemindersHeader")
        .text() mustBe "Cyfeiriad e-bost ar gyfer cyfathrebu'n ddigidol â CThEM"
      document.getElementsByTag("p").get(0).childNodes().get(0).toString() mustBe "Anfonir e-byst at: "
    }
  }
}
