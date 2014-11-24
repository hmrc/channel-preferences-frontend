package controllers.sa.prefs.internal

import connectors.{PreferencesConnector, SaPreference}
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.connectors.HeaderCarrier
import views.html.sa.prefs.email._
import uk.gov.hmrc.play.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait RemindersStatusPartialHtml {

  def preferencesConnector : PreferencesConnector

  def detailsStatus(utr: SaUtr)(implicit request: Request[_]): Future[HtmlFormat.Appendable] = {
    val resendVerificationUrl = controllers.sa.prefs.internal.routes.AccountDetailsController.resendValidationEmail().absoluteURL()
    implicit def hc = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)

    preferencesConnector.getPreferences(utr) map {
        case Some(SaPreference(true, Some(email))) => digital_true(email, resendVerificationUrl)
        case _ => digital_false()
    }
  }
}
