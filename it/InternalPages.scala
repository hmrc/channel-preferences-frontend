import java.net.URLEncoder

import org.openqa.selenium.WebDriver
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.endtoend.sa.{Page, ToAbsoluteUrl}

object InternalPages {
  trait InternalPage extends Page
  implicit def internalPageUrls[T <: InternalPage] = ToAbsoluteUrl.fromRelativeUrl[T](host = "localhost", port = 9000)

  object GenericUpgradePage {
    def apply[T](returnUrl: T)(implicit toAbsoluteUrl: ToAbsoluteUrl[T]) = new InternalPage {
      val title = "Go paperless with HMRC"

      def relativeUrl = "account/account-details/sa/upgrade-email-reminders?returnUrl=" +
        URLEncoder.encode(ApplicationCrypto.QueryParameterCrypto.encrypt(PlainText(toAbsoluteUrl.absoluteUrl(returnUrl))).value, "utf8")

      def `terms and conditions checkbox`(implicit driver: WebDriver) = checkbox("accept-tc").underlying
      def `no ask me later radio button`(implicit driver: WebDriver) = radioButton("opt-in-out").underlying
      def `yes continue electronic comms radio button`(implicit driver: WebDriver) = radioButton("opt-in-in").underlying
      def continue(implicit driver: WebDriver) = id("submitUpgrade")
    }
  }
}
