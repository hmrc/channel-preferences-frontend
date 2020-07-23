/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package views

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import uk.gov.hmrc.emailaddress.ObfuscatedEmailAddress
import views.html.account_details_update_email_address_thank_you

class AccountDetailsUpdateEmailAddressThankYouSpec
    extends PlaySpec with GuiceOneAppPerSuite with LanguageHelper with ConfigHelper {

  override implicit lazy val app: Application = fakeApp
  val template = app.injector.instanceOf[account_details_update_email_address_thank_you]

  "account details update emaill address thank you template" should {
    "render the correct content in english" in {
      val currentEmail = ObfuscatedEmailAddress("a@a.com")
      val foo = template(currentEmail)(engRequest, messagesInEnglish(), TestFixtures.sampleHostContext).toString()
      val document =
        Jsoup.parse(template(currentEmail)(engRequest, messagesInEnglish(), TestFixtures.sampleHostContext).toString())

      document.getElementsByClass("organisation-logo").first().text() mustBe "HM Revenue & Customs"
      document.getElementsByTag("title").first().text() mustBe "Verify your new email address"
      document.getElementsByTag("h1").get(0).text() mustBe "Verify your new email address"
      document.getElementsByTag("p").get(1).childNodes().get(0).toString mustBe "An email has been sent to "
      document
        .getElementById("verification-mail-message")
        .text() mustBe "Click on the link in the email to verify the address."
      document
        .getElementsByTag("p")
        .get(3)
        .text() mustBe "Until you do this, emails will continue to go to your old address."
      document
        .getElementById("return-to-dashboard-button")
        .attr("href") mustBe "/paperless/check-settings?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D"
      document
        .getElementById("return-to-dashboard-button")
        .text() mustBe "Continue"
    }

    "render the correct content in welsh" in {
      val currentEmail = ObfuscatedEmailAddress("a@a.com")
      val document =
        Jsoup.parse(template(currentEmail)(welshRequest, messagesInWelsh(), TestFixtures.sampleHostContext).toString())

      document.getElementsByClass("organisation-logo").first().text() mustBe "Cyllid a Thollau EM"
      document.getElementsByTag("title").first().text() mustBe "Dilysu'ch cyfeiriad e-bost newydd"
      document.getElementsByTag("h1").get(0).text() mustBe "Dilysu'ch cyfeiriad e-bost newydd"
      document.getElementsByTag("p").get(1).childNodes().get(0).toString mustBe "Anfonwyd e-bost at "
      document
        .getElementById("verification-mail-message")
        .text() mustBe "Cliciwch ar y cysylltiad yn yr e-bost er mwyn dilysu'r cyfeiriad."
      document
        .getElementsByTag("p")
        .get(3)
        .text() mustBe "Hyd nes eich bod yn gwneud hyn, bydd e-byst yn dal i gael eu hanfon i'ch hen gyfeiriad"
      document
        .getElementById("return-to-dashboard-button")
        .attr("href") mustBe "/paperless/check-settings?returnUrl=kvXgJfoJJ%2FbmaHgdHhhRpg%3D%3D&returnLinkText=huhgy5odc6KaXfFIMZXkeZjs11wvNGxKPz2CtY8L8GM%3D"
      document
        .getElementById("return-to-dashboard-button")
        .text() mustBe "Yn eich blaen"
    }
  }
}
