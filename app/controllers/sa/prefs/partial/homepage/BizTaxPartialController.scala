package controllers.sa.prefs.partial.homepage

import connectors.PreferencesConnector
import controllers.common.BaseController
import controllers.common.actions.Actions
import controllers.common.service.Connectors
import controllers.sa.prefs.SaRegimeWithoutRedirection
import play.api.mvc.Result
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.RunMode

import scala.concurrent.Future

class BizTaxPartialController(val preferenceConnector: PreferencesConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController with RunMode with Actions with RenderViewForPreferences {

  def this() = this(PreferencesConnector)(Connectors.authConnector)

  def pendingEmailVerification(utr: SaUtr)(implicit hc: HeaderCarrier): Future[Result] =
    preferenceConnector.getPreferences(utr).map(renderPrefs).map {
      case None => NoContent
      case Some(html) => Ok(html)
    }

  def preferencesWarning() = AuthorisedFor(regime = SaRegimeWithoutRedirection, redirectToOrigin = false).async {
    implicit user => implicit request => pendingEmailVerification(user.userAuthority.accounts.sa.get.utr)
  }
}
