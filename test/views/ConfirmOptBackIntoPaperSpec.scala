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
import views.html.confirm_opt_back_into_paper

class ConfirmOptBackIntoPaperSpec extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[confirm_opt_back_into_paper]

  "confirm opt back into paperless template" should {
    "render the correct content in english" in {
      val email = ObfuscatedEmailAddress("a@a.com")
      val document =
        Jsoup.parse(template(email)(engRequest, messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() mustBe "Stop notifications"
      document.getElementsByTag("h1").get(0).text() mustBe "Stop notifications"
      document.getElementsByTag("p").get(1).text() mustBe "You'll get letters again, instead of emails."
      document.getElementById("cancel-link").text() mustBe "Continue"
    }

    "render the correct content in welsh" in {
      val email = ObfuscatedEmailAddress("a@a.com")
      val document =
        Jsoup.parse(template(email)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() mustBe "Atal hysbysiadau"
      document.getElementsByTag("h1").get(0).text() mustBe "Atal hysbysiadau"
      document
        .getElementsByTag("p")
        .get(1)
        .text() mustBe "Byddwch yn cael llythyrau unwaith eto, yn hytrach nag e-byst."
      document.getElementById("cancel-link").text() mustBe "Yn eich blaen"
    }
  }
}
