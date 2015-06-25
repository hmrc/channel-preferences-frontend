package controllers.sa.prefs.internal

import connectors.PreferencesConnector
import controllers.sa.prefs.config.Global
import controllers.sa.prefs.{SaRegimeWithoutRedirection, UpgradeRemindersTandC}
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Request, Result}
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

trait UpgradeRemindersController extends FrontendController with Actions with AppName with PreferencesControllerHelper  {

  def authConnector: AuthConnector

  def preferencesConnector: PreferencesConnector

  def auditConnector: AuditConnector

  def display(): Action[AnyContent] = AuthorisedFor(SaRegimeWithoutRedirection).async {
    authContext => implicit request =>
      renderUpgradePageIfPreferencesAvailable(authContext.principal.accounts.sa.get.utr, authContext.principal.accounts.paye.map(_.nino))
  }

  private[controllers] def renderUpgradePageIfPreferencesAvailable(utr: SaUtr, maybeNino: Option[Nino])(implicit request: Request[AnyContent]): Future[Result] = {
    request.getQueryString("returnUrl") match {
      case Some(returnUrl) => decideOnPreferencesUI(utr,maybeNino, returnUrl, upgradeRemindersForm)
      case _ => Future.successful(BadRequest("returnUrl parameter missing"))
    }
  }

  def upgrade(returnUrl: String) = AuthorisedFor(SaRegimeWithoutRedirection).async {
    authContext => implicit request =>
      validateUpgradeForm(returnUrl, authContext.principal.accounts.sa.get.utr, authContext.principal.accounts.paye.map(_.nino)).map(response => response)
  }

  private[controllers] def validateUpgradeForm(returnUrl:String, utr: SaUtr, maybeNino: Option[Nino])(implicit request: Request[AnyContent]): Future[Result] = {

    upgradeRemindersForm.bindFromRequest()(request).fold(
      formWithErrors => {
        decideOnPreferencesUI(utr,maybeNino, returnUrl, formWithErrors)
      },
      formOK => {
        val digital = formOK.submitButton == "digital"
        upgradeTermsAndConditions(utr, maybeNino, digital).map(resp => Redirect(returnUrl))
      })
  }

  private def decideOnPreferencesUI(utr: SaUtr, maybeNino: Option[Nino], returnUrl:String, tandcForm:Form[UpgradeRemindersTandC])(implicit request: Request[AnyContent]) = { //} , headerCarrier: HeaderCarrier) = {

    preferencesConnector.getPreferences(utr, maybeNino).map {
      case Some(prefs) => Ok(upgrade_printing_preferences(utr, maybeNino, prefs.email.map(e => ObfuscatedEmailAddress(e.email)), returnUrl, tandcForm ))
      case _ => Redirect(returnUrl)
    }
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
