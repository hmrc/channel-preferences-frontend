package controllers.sa.prefs.internal

import connectors.{PreferencesConnector, SaEmailPreference}
import controllers.common.BaseController
import controllers.common.actions.Actions
import controllers.common.service.Connectors
import controllers.sa.prefs.SaRegimeWithoutRedirection
import play.api.Play.current
import play.api.mvc.Result
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.connectors.HeaderCarrier
import views.html.sa.prefs.warning.pending_email_verification

import scala.concurrent.Future

class BizTaxPartialController(val preferenceConnector: PreferencesConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController with RunMode
  with Actions {

  def this() = this(PreferencesConnector)(Connectors.authConnector)

  def pendingEmailVerification(utr: SaUtr)(implicit hc: HeaderCarrier): Future[Result] = {
    val accountDetailsUrl = play.api.Play.configuration.getString(s"govuk-tax.$env.platform.accountDetailsUrl")
        .getOrElse(throw new RuntimeException(s"Could not find account details URL under govuk-tax.$env.platform.accountDetailsUrl"))

    preferenceConnector.getPreferences(utr).map {
      case Some(prefs) if prefs.email.get.status != SaEmailPreference.Status.verified => Ok(pending_email_verification(prefs.email.get, accountDetailsUrl))
      case _ => NoContent
    }
  }

  def preferencesWarning() = AuthorisedFor(regime = SaRegimeWithoutRedirection, redirectToOrigin= false).async {
    implicit user => implicit request => pendingEmailVerification(user.userAuthority.accounts.sa.get.utr)
  }
}
