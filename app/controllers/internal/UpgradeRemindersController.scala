package controllers.internal

import authentication.SaRegimeWithoutRedirection
import config.Global
import connectors._
import model.Encrypted
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{EventTypes, ExtendedDataEvent}
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier
import views.html.sa.prefs.{upgrade_printing_preferences, upgrade_printing_preferences_thank_you}

import scala.concurrent.Future

object DeprecatedYTAUpgradeRemindersController extends UpgradeRemindersController {
  override def authConnector = Global.authConnector
  def preferencesConnector = PreferencesConnector

  override def auditConnector: AuditConnector = Global.auditConnector

  def displayUpgradeForm(encryptedReturnUrl: Encrypted[String]): Action[AnyContent] = AuthorisedFor(taxRegime = SaRegimeWithoutRedirection, pageVisibility = GGConfidence).async {
    authContext => implicit request =>
      _renderUpgradePageIfPreferencesAvailable(authContext.principal.accounts.sa.get.utr, authContext.principal.accounts.paye.map(_.nino), encryptedReturnUrl)
  }

  def submitUpgrade(returnUrl: Encrypted[String]) = AuthorisedFor(taxRegime = SaRegimeWithoutRedirection, pageVisibility = GGConfidence).async { authContext => implicit request =>
    _upgradePreferences(
      returnUrl = returnUrl.decryptedValue,
      utr = authContext.principal.accounts.sa.get.utr,
      maybeNino = authContext.principal.accounts.paye.map(_.nino)
    )
  }

  def displayUpgradeConfirmed(returnUrl: Encrypted[String]): Action[AnyContent] = AuthorisedFor(taxRegime = SaRegimeWithoutRedirection, pageVisibility = GGConfidence).async {
    authContext => implicit request =>
      _displayConfirm(returnUrl)
  }

}


object UpgradeRemindersController extends UpgradeRemindersController {
  override def authConnector = Global.authConnector
  def preferencesConnector = PreferencesConnector

  override def auditConnector: AuditConnector = Global.auditConnector

  def displayUpgradeForm(encryptedReturnUrl: Encrypted[String]): Action[AnyContent] = AuthorisedFor(taxRegime = SaRegimeWithoutRedirection, pageVisibility = GGConfidence).async {
    authContext => implicit request =>
      _renderUpgradePageIfPreferencesAvailable(authContext.principal.accounts.sa.get.utr, authContext.principal.accounts.paye.map(_.nino), encryptedReturnUrl)
  }

  def submitUpgrade(returnUrl: Encrypted[String]) = AuthorisedFor(taxRegime = SaRegimeWithoutRedirection, pageVisibility = GGConfidence).async { authContext => implicit request =>
    _upgradePreferences(
      returnUrl = returnUrl.decryptedValue,
      utr = authContext.principal.accounts.sa.get.utr,
      maybeNino = authContext.principal.accounts.paye.map(_.nino)
    )
  }

  def displayUpgradeConfirmed(returnUrl: Encrypted[String]): Action[AnyContent] = AuthorisedFor(taxRegime = SaRegimeWithoutRedirection, pageVisibility = GGConfidence).async {
    authContext => implicit request =>
      _displayConfirm(returnUrl)
  }
}

object UpgradeRemindersForm {
  def apply() = Form[Data](mapping(
    "opt-in" -> optional(boolean).verifying("sa_printing_preference.opt_in_choice_required", _.isDefined).transform[Boolean](_.get, Some(_)),
    "accept-tc" -> boolean
  )(Data.apply)(Data.unapply))

  case class Data(optIn: Boolean, acceptTandC: Boolean)
}

trait UpgradeRemindersController extends FrontendController with Actions with AppName {

  def authConnector: AuthConnector
  def preferencesConnector: PreferencesConnector
  def auditConnector: AuditConnector

  private[controllers] def _renderUpgradePageIfPreferencesAvailable(utr: SaUtr, maybeNino: Option[Nino], encryptedReturnUrl: Encrypted[String])(implicit request: Request[AnyContent]): Future[Result] = {
    decideRoutingFromPreference(utr,maybeNino, encryptedReturnUrl, UpgradeRemindersForm())
  }

  private[controllers] def _upgradePreferences(returnUrl:String, utr: SaUtr, maybeNino: Option[Nino])(implicit request: Request[AnyContent]): Future[Result] = {
    val form = UpgradeRemindersForm().bindFromRequest()
    form.fold(
      hasErrors = f => Future(BadRequest(upgrade_printing_preferences(None, Encrypted(returnUrl), f))),
      success = {
        case u @ UpgradeRemindersForm.Data(true, false) => Future(BadRequest(upgrade_printing_preferences(None, Encrypted(returnUrl), form.withError("accept-tc", "sa_printing_preference.accept_tc_required"))))
        case UpgradeRemindersForm.Data(true, true) => upgradePaperless(utr, maybeNino, Generic -> TermsAccepted(true)).map {
          case true => Redirect(routes.UpgradeRemindersController.displayUpgradeConfirmed(Encrypted(returnUrl)))
          case false => Redirect(returnUrl)
        }
        case UpgradeRemindersForm.Data(false, _) => upgradePaperless(utr, maybeNino, Generic -> TermsAccepted(false)).map(resp => Redirect(returnUrl))
      }
    )
  }

  private def decideRoutingFromPreference(utr: SaUtr, maybeNino: Option[Nino], encryptedReturnUrl: Encrypted[String], tandcForm:Form[UpgradeRemindersForm.Data])(implicit request: Request[AnyContent]) = {
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

  private def auditChoice(utr: SaUtr, nino: Option[Nino], terms: (TermsType, TermsAccepted))(implicit request: Request[_], hc: HeaderCarrier) =
    auditConnector.sendEvent(ExtendedDataEvent(
      auditSource = appName,
      auditType = EventTypes.Succeeded,
      tags = hc.toAuditTags("Set Print Preference", request.path),
      detail = Json.toJson(hc.toAuditDetails(
        "client" -> "",
        "nino" -> nino.map(_.nino).getOrElse("N/A"),
        "utr" -> utr.toString,
        "TandCsScope" -> terms._1.toString.toLowerCase,
        "userConfirmedReadTandCs" -> terms._2.accepted.toString,
        "journey" -> "",
        "digital" -> terms._2.accepted.toString,
        "cohort" -> ""
      ))))

  private[controllers] def _displayConfirm(returnUrl: Encrypted[String])(implicit request: Request[_], hc: HeaderCarrier) =
    Future(Ok(upgrade_printing_preferences_thank_you(returnUrl.decryptedValue)))

}
