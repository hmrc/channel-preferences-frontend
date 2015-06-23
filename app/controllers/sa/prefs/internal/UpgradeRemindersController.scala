package controllers.sa.prefs.internal

import connectors.PreferencesConnector
import controllers.sa.prefs.ExternalUrls.yourIncomeTax
import controllers.sa.prefs.SaRegimeWithoutRedirection
import controllers.sa.prefs.config.Global
import controllers.sa.prefs.internal.EmailOptInJourney._
import play.api.libs.json.Json
import play.api.mvc.{Result, Action, AnyContent, Request}
import play.twirl.api.Html
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.emailaddress.ObfuscatedEmailAddress
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{EventTypes, ExtendedDataEvent}
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.sa.prefs.upgrade_printing_preferences

import scala.concurrent.Future

object UpgradeRemindersController extends UpgradeRemindersController {
  override def authConnector = Global.authConnector
  def preferencesConnector = PreferencesConnector

  override def auditConnector: AuditConnector = Global.auditConnector
}

trait UpgradeRemindersController extends FrontendController with Actions with AppName  {

  def authConnector: AuthConnector

  def preferencesConnector: PreferencesConnector

  def auditConnector: AuditConnector

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
      val digital = request.body.asFormUrlEncoded.get("submitButton").head == "digital"
      upgradeTermsAndConditions(authContext.principal.accounts.sa.get.utr, authContext.principal.accounts.paye.map(_.nino), digital)
    }.map(_ => Redirect(returnUrl))
  }

  private[controllers] def upgradeTermsAndConditions(utr: SaUtr, nino: Option[Nino], digital: Boolean)(implicit request: Request[AnyContent], hc: HeaderCarrier) =
    preferencesConnector.upgradeTermsAndConditions(utr, digital).map {
      case true => auditChoice(utr, nino, true, digital)
    }

  private def auditChoice(utr: SaUtr, nino: Option[Nino], acceptedTAndCs:Boolean, digital: Boolean)(implicit request: Request[_], hc: HeaderCarrier) =
    auditConnector.sendEvent(ExtendedDataEvent(
      auditSource = appName,
      auditType = EventTypes.Succeeded,
      tags = hc.toAuditTags("Set Print Preference", request.path),
      detail = Json.toJson(hc.toAuditDetails(
        "client" -> "PAYETAI",
        "nino" -> nino.map(_.nino).getOrElse("N/A"),
        "utr" -> utr.toString,
        "TandCsScope" -> "Generic",
        "TandCsVersion" -> "V1",
        "userConfirmedReadTandCs" -> acceptedTAndCs.toString,
        "journey" -> "GenericUpgrade",
        "digital" -> digital.toString,
        "cohort" -> "TES_MVP"))))
}
