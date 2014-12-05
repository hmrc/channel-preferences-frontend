package controllers.sa.prefs.partial

import connectors.{PreferencesConnector, SaEmailPreference, SaPreference}
import controllers.common.BaseController
import controllers.common.actions.Actions
import controllers.common.service.Connectors
import controllers.sa.prefs.SaRegimeWithoutRedirection
import play.api.mvc.{Results, Result}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.RunMode
import views.html.sa.prefs.warning.{bounced_email, pending_email_verification}

import scala.concurrent.Future

trait RenderViewForPreferences extends Results {
  def renderPrefs(prefs: Option[SaPreference]): Option[HtmlFormat.Appendable] = prefs match {
    case Some(SaPreference(_, Some(email))) if email.status == SaEmailPreference.Status.pending => Some(pending_email_verification(email))
    case Some(SaPreference(_, Some(SaEmailPreference(_,status,true,_,_)))) if status == SaEmailPreference.Status.bounced => Some(bounced_email(true))
    case Some(SaPreference(_, Some(SaEmailPreference(_,status,false,_,_)))) if status == SaEmailPreference.Status.bounced => Some(bounced_email(false))
    case _ => None
  }
}

class BizTaxPartialController(val preferenceConnector: PreferencesConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController with RunMode with Actions with RenderViewForPreferences {

  def this() = this(PreferencesConnector)(Connectors.authConnector)

  def pendingEmailVerification(utr: SaUtr)(implicit hc: HeaderCarrier): Future[Result] = {
    preferenceConnector.getPreferences(utr).map(renderPrefs).map {
      case None => NoContent
      case Some(html) => Ok(html)
    }
  }

  def preferencesWarning() = AuthorisedFor(regime = SaRegimeWithoutRedirection, redirectToOrigin= false).async {
    implicit user => implicit request => pendingEmailVerification(user.userAuthority.accounts.sa.get.utr)
  }
}
