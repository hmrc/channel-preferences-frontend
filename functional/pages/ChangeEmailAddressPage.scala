package pages

import java.net.URLEncoder

import pages.InternalPagesSetup.InternalPage
import uk.gov.hmrc.crypto.{PlainText, ApplicationCrypto}
import uk.gov.hmrc.endtoend.sa.ToAbsoluteUrl

object ChangeEmailAddressPage {
  def apply[T](returnUrl: T, returnLinkText: String)(implicit toAbsoluteUrl: ToAbsoluteUrl[T]) = new InternalPage {
    val title = "Change your email address"
    def relativeUrl = "paperless/email-address/change?returnUrl=" + URLEncoder.encode(ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText(toAbsoluteUrl.absoluteUrl(returnUrl))).value, "utf8") + "&returnLinkText=" +
      URLEncoder.encode(ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText(returnLinkText)).value, "utf8")
  }

}
