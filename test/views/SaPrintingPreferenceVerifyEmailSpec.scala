/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package views

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import controllers.internal.OptInCohort
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.mvc.Call
import views.html.sa_printing_preference_verify_email

class SaPrintingPreferenceVerifyEmailSpec
    extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[sa_printing_preference_verify_email]

  "printing preferences verify email template" should {
    "render the correct content in english" in {
      val email = "a@a.com"

      val document = Jsoup.parse(
        template(email, OptInCohort.fromId(8).get, Call("GET", "/"), "redirectUrl")(
          engRequest,
          messagesInEnglish(),
          TestFixtures.sampleHostContext
        ).toString()
      )

      document.getElementsByTag("title").get(0).text() mustBe "Check your email address"
      document.getElementById("email-is-not-correct-link").text() mustBe "Change this email address"
    }

    "render the correct content in welsh" in {
      val email = "a@a.com"

      val document = Jsoup.parse(
        template(email, OptInCohort.fromId(8).get, Call("GET", "/"), "redirectUrl")(
          welshRequest,
          messagesInWelsh(),
          TestFixtures.sampleHostContext
        ).toString()
      )

      document.getElementsByTag("title").get(0).text() mustBe "Gwirio'ch cyfeiriad e-bost"
      document.getElementsByTag("h1").get(0).text() mustBe "Cofrestrwch ar gyfer hysbysiadau di-bapur"
      document.getElementsByTag("h2").get(0).text() mustBe "Gwirio'ch cyfeiriad e-bost"
      document.getElementsByTag("p").get(0).text() mustBe s"A ydych yn siŵr bod $email yn gywir?"
      document.getElementById("email-is-correct-link").text() mustBe "Mae'r cyfeiriad e-bost hwn yn gywir"
      document.getElementById("email-is-not-correct-link").text() mustBe "Newid y cyfeiriad e-bost hwn"
    }
  }
}
