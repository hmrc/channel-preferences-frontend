/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package views.sa.prefs

import _root_.helpers.{ ConfigHelper, LanguageHelper }
import controllers.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.test.FakeRequest
import views.html.sa.prefs.sa_printing_preference_verify_email_failed

class SaPrintingPreferenceVerifyEmailFailedSpec
    extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val saPrintingPreferenceVerifyEmailFailed = app.injector.instanceOf[sa_printing_preference_verify_email_failed]

  "printing preferences verify email failed template" should {
    "render the correct content in english" in {
      val document = Jsoup.parse(
        saPrintingPreferenceVerifyEmailFailed(None, None)(
          AuthenticatedRequest(FakeRequest("GET", "/"), None, None, None, None),
          messagesInEnglish()).toString())

      document.getElementsByTag("title").first().text() mustBe "Email address already verified"
    }

    "render the correct content in welsh" in {
      val document =
        Jsoup.parse(saPrintingPreferenceVerifyEmailFailed(None, None)(welshRequest, messagesInWelsh()).toString())

      document.getElementsByTag("title").first().text() mustBe "Cyfeiriad e-bost wedi'i ddilysu eisoes"
      document.getElementById("failure-heading").text() mustBe "Cyfeiriad e-bost wedi'i ddilysu eisoes"
      document.getElementById("success-message").text() mustBe "Mae'ch cyfeiriad e-bost wedi'i ddilysu eisoes."
      document.getElementById("link-to-home").child(0).text() mustBe "Yn eich blaen i'ch cyfrif ar-lein gyda CThEM"
    }
  }
}
