package pages

import java.net.URLEncoder

import pages.InternalPagesSetup.InternalPage
import uk.gov.hmrc.crypto.{PlainText, ApplicationCrypto}
import uk.gov.hmrc.endtoend.sa.ToAbsoluteUrl

object GenericUpgradeConfirmationPage {
  def apply[T](returnUrl: T)(implicit toAbsoluteUrl: ToAbsoluteUrl[T]) = new InternalPage {
    val title = "You're signed up"
    def relativeUrl = "paperless/upgrade/confirmed?returnUrl=" + URLEncoder.encode(ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText(toAbsoluteUrl.absoluteUrl(returnUrl))).value, "utf8")

    def `continue button` = className("button")
  }
}
