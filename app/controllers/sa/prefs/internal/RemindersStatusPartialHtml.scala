package controllers.sa.prefs.internal


import connectors.PreferencesConnector
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.connectors.HeaderCarrier
import views.html.account_details_partial
import uk.gov.hmrc.play.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait RemindersStatusPartialHtml {

  def preferencesConnector : PreferencesConnector

  def detailsStatus()(implicit user: User, request: Request[_], hc: HeaderCarrier): Future[HtmlFormat.Appendable] = {
    user.userAuthority.accounts.sa match {
      case Some(sa) => preferencesConnector.getPreferences(sa.utr) flatMap { preferences =>
        Future.successful(account_details_partial(preferences))
      }
    }
  }
}
