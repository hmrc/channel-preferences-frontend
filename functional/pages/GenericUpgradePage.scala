package pages

import java.net.URLEncoder

import org.openqa.selenium.WebDriver
import pages.InternalPagesSetup.InternalPage
import uk.gov.hmrc.crypto.ApplicationCrypto.QueryParameterCrypto
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.endtoend.sa.AbsoluteUrl

object GenericUpgradePage {
  def apply[T](returnUrl: T)(implicit toAbsoluteUrl: AbsoluteUrl[T]) = new InternalPage {
    val title = "Go paperless with HMRC"
    def relativeUrl = "paperless/upgrade?returnUrl=" + URLEncoder.encode(QueryParameterCrypto.encrypt(PlainText(toAbsoluteUrl(returnUrl))).value, "utf8")

    def `terms and conditions checkbox`(implicit driver: WebDriver) = checkbox("accept-tc").underlying
    def `no ask me later radio button`(implicit driver: WebDriver) = radioButton("opt-in-out").underlying
    def `yes continue electronic comms radio button`(implicit driver: WebDriver) = radioButton("opt-in-in").underlying
    def continue(implicit driver: WebDriver) = id("submitUpgrade")
    def `provided email address`(implicit driver: WebDriver) = id("opted-in-email").element.text
  }
}
