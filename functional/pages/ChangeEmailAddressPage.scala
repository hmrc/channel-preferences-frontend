package pages

import java.net.URLEncoder

import pages.InternalPagesSetup.InternalPage
import uk.gov.hmrc.crypto.ApplicationCrypto.QueryParameterCrypto
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.endtoend.sa.AbsoluteUrl

object ChangeEmailAddressPage {
  def apply[T](returnUrl: T, returnLinkText: String)(implicit toAbsoluteUrl: AbsoluteUrl[T]) = new InternalPage {
    val title = "Change your email address"
    def relativeUrl = "paperless/email-address/change?returnUrl=" + URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(toAbsoluteUrl(returnUrl))).value, "utf8") + "&returnLinkText=" +
      URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(returnLinkText)).value, "utf8")
  }

}
