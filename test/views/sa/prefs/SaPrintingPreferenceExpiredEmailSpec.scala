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
import views.html.sa.prefs.sa_printing_preference_expired_email

class SaPrintingPreferenceExpiredEmailSpec
    extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val saPrintingPreferenceExpiredEmail =
    app.injector.instanceOf[sa_printing_preference_expired_email]

  "printing preferences expired emai; template" should {
    "render the correct content in english" in {
      val foo = saPrintingPreferenceExpiredEmail()(engRequest, messagesInEnglish()).toString()
      val document = Jsoup.parse(saPrintingPreferenceExpiredEmail()(engRequest, messagesInEnglish()).toString())

      document
        .getElementById("link-to-home")
        .childNodes()
        .get(2)
        .toString
        .trim() mustBe "and request a new verification link"
    }

    "render the correct content in welsh" in {
      val document = Jsoup.parse(saPrintingPreferenceExpiredEmail()(welshRequest, messagesInWelsh()).toString())

      document.getElementsByTag("title").first().text() mustBe "NID yw'ch cyfeiriad e-bost wedi'i ddilysu"
      document
        .getElementById("link-to-home")
        .childNodes()
        .get(1)
        .childNode(0)
        .toString mustBe "Yn eich blaen i'ch cyfrif ar-lein gyda CThEM"
      document
        .getElementById("link-to-home")
        .childNodes()
        .get(2)
        .toString
        .trim() mustBe "a gwnewch gais am gysylltiad dilysu newydd"
    }
  }
}
