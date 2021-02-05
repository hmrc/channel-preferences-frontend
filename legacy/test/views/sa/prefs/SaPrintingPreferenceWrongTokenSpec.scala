/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package views.sa.prefs

import _root_.helpers.{ ConfigHelper, LanguageHelper }
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import views.html.sa.prefs.sa_printing_preference_wrong_token

class SaPrintingPreferenceWrongTokenSpec
    extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[sa_printing_preference_wrong_token]

  "sa printing preferences wrong token template" should {
    "render the correct content in english" in {
      val document = Jsoup.parse(template()(engRequest, messagesInEnglish()).toString())

      document.getElementsByTag("title").first().text() mustBe "You've used a link that has now expired"
    }

    "render the correct content in welsh" in {
      val document = Jsoup.parse(template()(welshRequest, messagesInWelsh()).toString())

      document
        .getElementsByTag("title")
        .first()
        .text() mustBe "Rydych wedi defnyddio cysylltiad sydd bellach wedi dod i ben"
      document
        .getElementById("failure-heading")
        .text() mustBe "Rydych wedi defnyddio cysylltiad sydd bellach wedi dod i ben"
      document
        .getElementById("success-message")
        .text() mustBe "Mae'n bosibl ei fod wedi cael ei anfon i hen gyfeiriad e-bost neu un gwahanol. Dylech ddefnyddio'r cysylltiad yn yr e-bost dilysu diwethaf a anfonwyd i'r cyfeiriad e-bost a nodwyd gennych."
    }
  }
}
