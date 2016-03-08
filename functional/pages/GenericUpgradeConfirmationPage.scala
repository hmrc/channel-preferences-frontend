package pages

import java.net.URLEncoder

import pages.InternalPagesSetup.InternalPage
import uk.gov.hmrc.crypto.ApplicationCrypto.QueryParameterCrypto
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.endtoend.sa.AbsoluteUrl

object GenericUpgradeConfirmationPage {
  def apply[T](returnUrl: T)(implicit toAbsoluteUrl: AbsoluteUrl[T]) = new InternalPage {
    val title = "You're signed up"
    def relativeUrl = "paperless/upgrade/confirmed?returnUrl=" + URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(toAbsoluteUrl(returnUrl))).value, "utf8")

    def `continue button` = className("button")
  }
}
