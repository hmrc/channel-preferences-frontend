package pages

import java.net.URLEncoder

import org.openqa.selenium.WebDriver
import pages.InternalPagesSetup.InternalPage
import uk.gov.hmrc.crypto.ApplicationCrypto.QueryParameterCrypto
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.endtoend.sa.AbsoluteUrl

object ChangeEmailAddressPage {
  def apply[T](returnUrl: T, returnLinkText: String)(implicit toAbsoluteUrl: AbsoluteUrl[T]) = new InternalPage {
    val title = "Change your email address"
    val relativeUrl = "paperless/email-address/change?returnUrl=" + URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(toAbsoluteUrl(returnUrl))).value, "utf8") + "&returnLinkText=" +
      URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(returnLinkText)).value, "utf8")

    def `email textbox`(implicit driver: WebDriver) = emailField("email.main")
    def `confirm email textbox`(implicit driver: WebDriver) = emailField("email.confirm")
    def `change email validation message`(implicit driver: WebDriver) = find(id("email.main-error")).get.text

    def completeForm(emailAddress: String, confirmEmail: Option[String] = None)(implicit driver: WebDriver): Unit = {
      `email textbox`.value = emailAddress
      confirmEmail match {
        case None => `confirm email textbox`.value = emailAddress
        case s:Some[String] => `confirm email textbox`.value = s.get
      }
      submit()
    }
  }
}

object CheckChangedEmailAddressPage {
  def apply[T](returnUrl: T, returnLinkText: String)(implicit toAbsoluteUrl: AbsoluteUrl[T]) = new InternalPage {
    val title = "Check your email address"
    val relativeUrl = "paperless/email-address/change?returnUrl=" + URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(toAbsoluteUrl(returnUrl))).value, "utf8") + "&returnLinkText=" +
      URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(returnLinkText)).value, "utf8")
    def changedEmailIsCorrectLink(implicit driver: WebDriver) =    id("emailIsCorrectLink")
    def changedEmailIsNotCorrectLink(implicit driver: WebDriver) = id("emailIsNotCorrectLink")
  }
}

object ConfirmationOfChangedEmailAddressPage {
  def apply[T](returnUrl: T, returnLinkText: String)(implicit toAbsoluteUrl: AbsoluteUrl[T]) = new InternalPage {
    val title = "Verify your new email address"
    val relativeUrl = "paperless/email-address/change/confirmed?returnUrl=" + URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(toAbsoluteUrl(returnUrl))).value, "utf8") + "&returnLinkText=" +
      URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(returnLinkText)).value, "utf8")
  }
}