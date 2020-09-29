/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package views

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import controllers.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import uk.gov.hmrc.emailaddress.ObfuscatedEmailAddress
import org.scalatestplus.play.PlaySpec
import views.html.opted_back_into_paper_thank_you

class OptedBackIntoPaperThankYouSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[opted_back_into_paper_thank_you]

  "opted back into paper template" should {
    "render the correct content in english" in {
      val email = ObfuscatedEmailAddress("a@a.com")
      val document = Jsoup.parse(template()(engRequest, messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").get(0).text() mustBe "Paperless notifications have stopped"
      document.getElementsByTag("h1").get(0).text() mustBe "Paperless notifications have stopped"
      document.getElementsByTag("p").get(0).text() mustBe "You'll get letters again, instead of emails."
      document.getElementsByTag("p").get(1).text() mustBe "You've been sent an email confirming this."
      document.getElementsByTag("p").get(2).text() mustBe "You can go paperless again at any time."
    }

    "render the correct content in welsh" in {
      val email = ObfuscatedEmailAddress("a@a.com")
      val document = Jsoup.parse(template()(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").get(0).text() mustBe "Mae hysbysiadau di-bapur wedi dod i ben"
      document.getElementsByTag("h1").get(0).text() mustBe "Mae hysbysiadau di-bapur wedi dod i ben"
      document
        .getElementsByTag("p")
        .get(0)
        .text() mustBe "Byddwch yn cael llythyrau unwaith eto, yn hytrach nag e-byst."
      document.getElementsByTag("p").get(1).text() mustBe "Anfonwyd e-bost atoch yn cadarnhau hyn."
      document.getElementsByTag("p").get(2).text() mustBe "Gallwch fynd yn ddi-bapur unwaith eto ar unrhyw adeg."
    }
  }
}
