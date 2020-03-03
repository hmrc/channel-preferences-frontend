/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package views.sa.prefs

import _root_.helpers.{ ConfigHelper, LanguageHelper }
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import views.html.sa.prefs.sa_printing_preference_verify_email

class SaPrintingPreferenceVerifyEmailSpec
    extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[sa_printing_preference_verify_email]

  "printing preferences verify email template" should {
    "render the correct content in english" in {
      val document = Jsoup.parse(template(None, None)(engRequest, messagesInEnglish()).toString())

      document.getElementsByTag("title").first().text() mustBe "Email address verified"
    }

    "render the correct content in welsh" in {
      val document = Jsoup.parse(template(None, None)(welshRequest, messagesInWelsh()).toString())

      document.getElementsByTag("title").first().text() mustBe "Cyfeiriad e-bost wedi'i ddilysu"
      document.getElementById("success-heading").text() mustBe "Cyfeiriad e-bost wedi'i ddilysu"
      document
        .getElementById("success-message")
        .text() mustBe "Rydych nawr wedi cofrestru ar gyfer hysbysiadau di-bapur."
      document.getElementById("link-to-home").child(0).text() mustBe "Yn eich blaen i'ch cyfrif ar-lein gyda CThEM"
    }
  }
}
