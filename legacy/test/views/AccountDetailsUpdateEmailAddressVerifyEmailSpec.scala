/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package views

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import views.html.account_details_update_email_address_verify_email

class AccountDetailsUpdateEmailAddressVerifyEmailSpec
    extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[account_details_update_email_address_verify_email]

  "account details update email address verify email template" should {
    "render the correct content in english" in {
      val email = "a@a.com"
      val document =
        Jsoup.parse(template(email)(engRequest, messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() mustBe "Check your email address"
      document.getElementsByTag("h1").get(0).text() mustBe "Check your email address"
      document.getElementsByTag("p").get(0).text() mustBe s"Are you sure $email is correct?"
      document.getElementById("emailIsCorrectLink").text() mustBe "This email address is correct"
      document.getElementById("emailIsNotCorrectLink").text() mustBe "Change this email address"
    }

    "render the correct content in welsh" in {
      val email = "a@a.com"
      val document =
        Jsoup.parse(template(email)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() mustBe "Gwirio'ch cyfeiriad e-bost"
      document.getElementsByTag("h1").get(0).text() mustBe "Gwirio'ch cyfeiriad e-bost"
      document.getElementsByTag("p").get(0).text() mustBe s"A ydych yn siŵr bod $email yn gywir?"
      document.getElementById("emailIsCorrectLink").text() mustBe "Mae'r cyfeiriad e-bost hwn yn gywir"
      document.getElementById("emailIsNotCorrectLink").text() mustBe "Newid y cyfeiriad e-bost hwn"
    }
  }
}
