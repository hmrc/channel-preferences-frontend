package controllers.sa.prefs.partial

import connectors.{PreferencesConnector, SaEmailPreference, SaPreference}
import controllers.common.BaseController
import controllers.common.actions.Actions
import controllers.common.service.Connectors
import controllers.sa.prefs.SaRegimeWithoutRedirection
import play.api.mvc.Result
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.RunMode
import views.html.sa.prefs.warning.pending_email_verification

import scala.concurrent.Future

class BizTaxPartialController(val preferenceConnector: PreferencesConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController with RunMode
  with Actions {

  def this() = this(PreferencesConnector)(Connectors.authConnector)

  def pendingEmailVerification(utr: SaUtr)(implicit hc: HeaderCarrier): Future[Result] = {

    preferenceConnector.getPreferences(utr).map {
      case Some(SaPreference(_, Some(email))) if email.status != SaEmailPreference.Status.verified =>
        Ok(pending_email_verification(email))
      case _ => NoContent
    }
  }

  def preferencesWarning() = AuthorisedFor(regime = SaRegimeWithoutRedirection, redirectToOrigin= false).async {
    implicit user => implicit request => pendingEmailVerification(user.userAuthority.accounts.sa.get.utr)
  }
}
