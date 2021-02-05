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
import views.html.account_details_verification_email_resent_confirmation

class AccountDetailsVerificationEmailResentConfirmationSpec
    extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[account_details_verification_email_resent_confirmation]

  "account details verification email resent confirmation template" should {
    "render the correct content in english" in {
      val email = "a@a.com"
      val document =
        Jsoup.parse(template(email)(engRequest, messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() mustBe "Verification email sent"
      document.getElementsByTag("h1").get(0).text() mustBe "Verification email sent"
      document
        .getElementById("verification-mail-message")
        .text() mustBe s"A new email has been sent to $email. Click on the link in the email to verify your email address with HMRC."
      document
        .getElementById("return-to-dashboard-button")
        .attr(
          "href"
        ) mustBe "/paperless/check-settings?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D"
      document
        .getElementById("return-to-dashboard-button")
        .text() mustBe "Continue"
    }

    "render the correct content in welsh" in {
      val email = "a@a.com"
      val document =
        Jsoup.parse(template(email)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() mustBe "E-bost dilysu wedi'i anfon"
      document.getElementsByTag("h1").get(0).text() mustBe "E-bost dilysu wedi'i anfon"
      document
        .getElementById("verification-mail-message")
        .text() mustBe s"Mae e-bost newydd wedi'i anfon i $email. Cliciwch ar y cysylltiad yn yr e-bost er mwyn dilysu'ch cyfeiriad e-bost gyda CThEM."
      document
        .getElementById("return-to-dashboard-button")
        .attr(
          "href"
        ) mustBe "/paperless/check-settings?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D"
      document
        .getElementById("return-to-dashboard-button")
        .text() mustBe "Yn eich blaen"
    }
  }
}
