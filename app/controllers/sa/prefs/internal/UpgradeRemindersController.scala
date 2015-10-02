package controllers.sa.prefs.internal

import connectors._
import controllers.sa.prefs.Encrypted
import controllers.sa.prefs.config.Global
import controllers.sa.prefs.{SaRegimeWithoutRedirection, UpgradeRemindersTandC}
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.crypto.{PlainText, ApplicationCrypto}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{EventTypes, ExtendedDataEvent}
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.sa.prefs.{upgrade_printing_preferences, upgrade_printing_preferences_thank_you}

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

  def display(encryptedReturnUrl: Encrypted[String]): Action[AnyContent] = AuthorisedFor(SaRegimeWithoutRedirection).async {
    authContext => implicit request =>
      renderUpgradePageIfPreferencesAvailable(authContext.principal.accounts.sa.get.utr, authContext.principal.accounts.paye.map(_.nino), encryptedReturnUrl)
  }

  private[controllers] def renderUpgradePageIfPreferencesAvailable(utr: SaUtr, maybeNino: Option[Nino], encryptedReturnUrl: Encrypted[String])(implicit request: Request[AnyContent]): Future[Result] = {
    decideRoutingFromPreference(utr,maybeNino, encryptedReturnUrl, upgradeRemindersForm)
  }

  def upgrade(returnUrl: Encrypted[String]) = AuthorisedFor(SaRegimeWithoutRedirection).async { authContext => implicit request =>
    upgradePreferences(
      returnUrl = returnUrl.decryptedValue,
      utr = authContext.principal.accounts.sa.get.utr,
      maybeNino = authContext.principal.accounts.paye.map(_.nino)
    )
  }

  private[controllers] def upgradePreferences(returnUrl:String, utr: SaUtr, maybeNino: Option[Nino])(implicit request: Request[AnyContent]): Future[Result] = {
    if (upgradeRemindersForm.bindFromRequest()(request).get.isDigitalButtonSelected) {
      upgradePaperless(utr, maybeNino, Generic -> TermsAccepted(true)).map {
        case true => Redirect(routes.UpgradeRemindersController.thankYou(Encrypted(returnUrl)))
        case false => Redirect(returnUrl)
      }
    }
    else {
      upgradePaperless(utr, maybeNino, Generic -> TermsAccepted(false)).map(resp => Redirect(returnUrl))
    }
  }

  private def decideRoutingFromPreference(utr: SaUtr, maybeNino: Option[Nino], encryptedReturnUrl: Encrypted[String], tandcForm:Form[UpgradeRemindersTandC])(implicit request: Request[AnyContent]) = {
    preferencesConnector.getPreferences(utr, maybeNino).map {
      case Some(prefs) => Ok(upgrade_printing_preferences(prefs.email.map(e => e.email), encryptedReturnUrl, tandcForm))
      case None => Redirect(encryptedReturnUrl.decryptedValue)
    }
  }

  private[controllers] def upgradePaperless(utr: SaUtr, nino: Option[Nino], termsAccepted: (TermsType, TermsAccepted))(implicit request: Request[AnyContent], hc: HeaderCarrier) : Future[Boolean] =
    preferencesConnector.addTermsAndConditions(utr, termsAccepted, email = None).map { success =>
      if (success) auditChoice(utr, nino, termsAccepted)
      success
    }

  private def auditChoice(utr: SaUtr, nino: Option[Nino], termsAccepted: (TermsType, TermsAccepted))(implicit request: Request[_], hc: HeaderCarrier) =
    auditConnector.sendEvent(ExtendedDataEvent(
      auditSource = appName,
      auditType = EventTypes.Succeeded,
      tags = hc.toAuditTags("Set Print Preference", request.path),
      detail = Json.toJson(hc.toAuditDetails(
        "client" -> "PAYETAI",
        "nino" -> nino.map(_.nino).getOrElse("N/A"),
        "utr" -> utr.toString,
        "TandCsScope" -> termsAccepted._1.toString.toLowerCase,
        "userConfirmedReadTandCs" -> "true",
        "journey" -> "",
        "digital" -> termsAccepted._2.accepted.toString,
        "cohort" -> ""
      ))))


  def thankYou(returnUrl: Encrypted[String]): Action[AnyContent] = AuthorisedFor(SaRegimeWithoutRedirection).async {
    authContext => implicit request =>
        Future(Ok(upgrade_printing_preferences_thank_you(returnUrl.decryptedValue)))
    }
}
