/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package views

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import controllers.internal.EmailForm
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import views.html.account_details_update_email_address

class AccountDetailsUpdateEmailAddressSpec
    extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp

  val template = app.injector.instanceOf[account_details_update_email_address]
  "account details update email address template" should {
    "render the correct content in english" in {
      val currentEmail = "a@a.com"
      val form = EmailForm()
      val document = Jsoup.parse(
        template(currentEmail, form)(engRequest, messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() mustBe "Change your email address"
      document.getElementsByTag("h1").get(0).text() mustBe "Change your email address"
      document.getElementsByTag("p").get(1).childNodes().get(0).toString mustBe "Emails are sent to "
      document.getElementById("submit-email-button").text() mustBe "Change email address"
      document.getElementById("cancel-link").text() mustBe "Continue"
      document.getElementsByAttributeValue("for", "email.main").first().child(0).text() mustBe "New email address"
      document
        .getElementsByAttributeValue("for", "email.confirm")
        .first()
        .child(0)
        .text() mustBe "Confirm new email address"
      document
        .getElementById("cancel-link").attr("href") mustBe "/paperless/check-settings?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D"
      document
        .getElementById("cancel-link")
        .text() mustBe "Continue"

    }

    "render the correct content in welsh" in {
      val currentEmail = "a@a.com"
      val form = EmailForm()
      val document = Jsoup.parse(
        template(currentEmail, form)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").first().text() mustBe "Newid eich cyfeiriad e-bost"
      document.getElementsByTag("h1").get(0).text() mustBe "Newid eich cyfeiriad e-bost"
      document.getElementsByTag("p").get(1).childNodes().get(0).toString mustBe "Anfonir e-byst at "
      document.getElementById("submit-email-button").text() mustBe "Newid y cyfeiriad e-bost"
      document.getElementById("cancel-link").text() mustBe "Yn eich blaen"
      document.getElementsByAttributeValue("for", "email.main").first().child(0).text() mustBe "Cyfeiriad e-bost newydd"
      document
        .getElementsByAttributeValue("for", "email.confirm")
        .first()
        .child(0)
        .text() mustBe "Cadarnhau'r cyfeiriad e-bost newydd"

      document
        .getElementById("cancel-link").attr("href") mustBe "/paperless/check-settings?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D"
      document
        .getElementById("cancel-link")
        .text() mustBe "Yn eich blaen"
    }
  }
}
