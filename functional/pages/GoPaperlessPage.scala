package pages

import java.net.URLEncoder

import org.openqa.selenium.WebDriver
import pages.InternalPagesSetup.InternalPage
import uk.gov.hmrc.crypto.ApplicationCrypto.QueryParameterCrypto
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.endtoend.sa.AbsoluteUrl

object GoPaperlessPage {
  def apply[T](returnUrl: T, returnLinkText: String)(implicit toAbsoluteUrl: AbsoluteUrl[T]) = new InternalPage {
    val title = "Go paperless with HMRC"
    def relativeUrl = "paperless/choose/8?returnUrl=" + URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(toAbsoluteUrl(returnUrl))).value, "utf8") + "&returnLinkText=" +
      URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(returnLinkText)).value, "utf8")

    def `email textbox`(implicit driver: WebDriver) = emailField("email.main")
    def `confirm email textbox`(implicit driver: WebDriver) = emailField("email.confirm")
    def `terms and conditions checkbox`(implicit driver: WebDriver) = checkbox("accept-tc")
    def `go paperless validation message`(implicit driver: WebDriver) =
      find(cssSelector(".error-notification")).get.text
    def `continue button`(implicit driver: WebDriver) = name("submitButton")
    def `no I don't want to sign up radio button`(implicit driver: WebDriver) = radioButton("opt-in-out").underlying
    def emailReminderFormClass(implicit driver: WebDriver) =
      cssSelector("#form-submit-email-address > fieldset").element.attribute("class").toString
    def `yes send by email radio button`(implicit driver: WebDriver) = radioButton("opt-in-in").underlying

    def completeForm(emailAddress: String, confirmEmail: Option[String] = None, TsAndCsSelected:Boolean = true)(implicit driver: WebDriver) {
      `email textbox`.value = emailAddress
      confirmEmail match {
        case None => `confirm email textbox`.value = emailAddress
        case s:Some[String] => `confirm email textbox`.value = s.get
      }
      if(TsAndCsSelected) `terms and conditions checkbox`.select()
      submit()
    }

    def submitGoPaperlessForm(implicit driver: WebDriver) = submit()
  }
}
