package controllers.sa.prefs.internal

import connectors.PreferencesConnector
import controllers.sa.prefs.ExternalUrls.yourIncomeTax
import controllers.sa.prefs.SaRegimeWithoutRedirection
import controllers.sa.prefs.config.Global
import play.api.mvc.{Result, Action, AnyContent, Request}
import play.twirl.api.Html
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.emailaddress.ObfuscatedEmailAddress
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.sa.prefs.upgrade_printing_preferences

import scala.concurrent.Future

object UpgradeRemindersController extends UpgradeRemindersController {
  override def authConnector = Global.authConnector
  def preferencesConnector = PreferencesConnector
}

trait UpgradeRemindersController extends FrontendController with Actions {

  def authConnector: AuthConnector

  def preferencesConnector: PreferencesConnector

  def display(): Action[AnyContent] = AuthorisedFor(SaRegimeWithoutRedirection).async {
    authContext => implicit request =>
      renderUpgradePageIfPreferencesAvailable(authContext.principal.accounts.sa.get.utr, authContext.principal.accounts.paye.map(_.nino))
  }

  private[controllers] def renderUpgradePageIfPreferencesAvailable(utr: SaUtr, maybeNino: Option[Nino])(implicit request: Request[AnyContent]): Future[Result] = {
    request.getQueryString("returnUrl") match {
      case Some(returnUrl) =>
        preferencesConnector.getPreferences(utr, maybeNino).map {
          case Some(prefs) => Ok(upgrade_printing_preferences(utr, maybeNino, prefs.email.map(e => ObfuscatedEmailAddress(e.email)), returnUrl))
          case _ => NotFound
        }
      case _ => Future.successful(BadRequest("returnUrl parameter missing"))
    }

  }

  def upgrade(returnUrl: String) = AuthorisedFor(SaRegimeWithoutRedirection).async {
    authContext => implicit request => {
      val accepted = request.body.asFormUrlEncoded.get("submitButton").head == "accepted"
      preferencesConnector.upgradeTermsAndConditions(authContext.principal.accounts.sa.get.utr, accepted)
    }.map(_ => Redirect(returnUrl))
  }
}
