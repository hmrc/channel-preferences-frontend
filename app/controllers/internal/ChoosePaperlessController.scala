package controllers.internal

import config.Global
import connectors._
import controllers.internal.EmailOptInJourney._
import controllers.{Authentication, internal}
import model.{Encrypted, HostContext}
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{EventTypes, ExtendedDataEvent}
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

object ChoosePaperlessController extends ChoosePaperlessController with ChoosePaperlessControllerDependencies {

  def redirectToDisplayFormWithCohort(implicit emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = authenticated { implicit authContext => implicit request =>
    _redirectToDisplayFormWithCohort(emailAddress)
  }

  def displayForm(implicit cohort: Option[OptInCohort], emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = authenticated.async { implicit user => implicit request =>
    _displayForm(AccountDetails, emailAddress, cohort)
  }

  def submitForm(implicit hostContext: HostContext) = authenticated.async { implicit user => implicit request =>
    _submitForm(AccountDetails)
  }

  def displayNearlyDone(implicit emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = authenticated { implicit authContext => implicit request =>
    _displayNearlyDone(emailAddress)
  }
}

trait ChoosePaperlessControllerDependencies extends AppName with OptInCohortCalculator with Authentication{
  this: ChoosePaperlessController =>

  override val auditConnector = Global.auditConnector
  override val preferencesConnector = PreferencesConnector
  override val emailConnector = EmailConnector
  override protected implicit def authConnector: AuthConnector = Global.authConnector
}

trait ChoosePaperlessController
  extends FrontendController
  with Actions
  with AppName {

  def preferencesConnector: PreferencesConnector
  def emailConnector: EmailConnector
  def auditConnector: AuditConnector

  def calculateCohort(authContext: AuthContext): OptInCohort

  private[controllers] def _redirectToDisplayFormWithCohort(emailAddress: Option[Encrypted[EmailAddress]])(implicit authContext: AuthContext, hostContext: HostContext) =
    Redirect(routes.ChoosePaperlessController.displayForm(Some(calculateCohort(authContext)), emailAddress, hostContext))

  private[controllers] def _displayForm(journey: Journey, emailAddress: Option[Encrypted[EmailAddress]], possibleCohort: Option[OptInCohort])(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext) = {
    val saUtr = authContext.principal.accounts.sa.get.utr
    possibleCohort.fold(ifEmpty = Future.successful(_redirectToDisplayFormWithCohort(emailAddress))) { cohort =>
      preferencesConnector.saveCohort(saUtr, calculateCohort(authContext)).map { case _ =>
        auditPageShown(saUtr, journey, cohort)
        val email = emailAddress.map(_.decryptedValue)
        Ok(
          views.html.sa.prefs.sa_printing_preference(
            emailForm = OptInDetailsForm().fill(OptInDetailsForm.Data(emailAddress = email, preference = email.map(_ => OptInDetailsForm.Data.PaperlessChoice.OptedIn), acceptedTcs = Some(false))),
            submitPrefsFormAction = internal.routes.ChoosePaperlessController.submitForm(hostContext),
            cohort = cohort
          )
        )
      }
    }
  }

  private[controllers] def _submitForm(journey: Journey)(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext) = {
    val cohort = calculateCohort(authContext)
    val saUtr = authContext.principal.accounts.sa.get.utr

    def saveAndAuditPreferences(utr:SaUtr, digital: Boolean, email: Option[String], hc: HeaderCarrier): Future[Result] = {
      implicit val headerCarrier = hc
      val terms = Generic -> TermsAccepted(digital)
      for {
        _ <- preferencesConnector.saveCohort(utr, calculateCohort(authContext))
        preferencesStatus <- preferencesConnector.updateTermsAndConditions(utr, terms, email)
      } yield {
        auditChoice(utr, journey, cohort, terms, email, preferencesStatus)
        digital match {
          case true  => Redirect(routes.ChoosePaperlessController.displayNearlyDone(email map (emailAddress => Encrypted(EmailAddress(emailAddress))), hostContext))
          case false => Redirect(hostContext.returnUrl)
        }
      }
    }

    def returnToFormWithErrors(f: Form[_]) = Future.successful(BadRequest(views.html.sa.prefs.sa_printing_preference(f, routes.ChoosePaperlessController.submitForm(hostContext), cohort)))

    OptInOrOutForm().bindFromRequest.fold[Future[Result]](
      hasErrors = returnToFormWithErrors,
      happyForm =>
        if (happyForm.optedIn.contains(false)) saveAndAuditPreferences(saUtr, false, None, hc)
        else OptInDetailsForm().bindFromRequest.fold[Future[Result]](
          hasErrors = returnToFormWithErrors,
          success = {
            case emailForm@OptInDetailsForm.Data((Some(emailAddress), _), _, Some(OptInDetailsForm.Data.PaperlessChoice.OptedIn), Some(true)) =>
              val emailVerificationStatus =
                if (emailForm.isEmailVerified) Future.successful(true)
                else emailConnector.isValid(emailAddress)

              emailVerificationStatus.flatMap {
                case true => saveAndAuditPreferences(saUtr, true, Some(emailAddress), hc)
                case false => Future.successful(Ok(views.html.sa_printing_preference_verify_email(emailAddress, cohort)))
              }
            case _ => returnToFormWithErrors(OptInDetailsForm().bindFromRequest)
          }
        )
    )
  }

  private def auditPageShown(utr: SaUtr, journey: Journey, cohort: OptInCohort)(implicit request: Request[_], hc: HeaderCarrier) =
    auditConnector.sendEvent(ExtendedDataEvent(
      auditSource = appName,
      auditType = EventTypes.Succeeded,
      tags = hc.toAuditTags("Show Print Preference Option", request.path),
      detail = Json.toJson(hc.toAuditDetails(
        "utr" -> utr.toString,
        "journey" -> journey.toString,
        "cohort" -> cohort.toString))))

  private def auditChoice(utr: SaUtr, journey: Journey, cohort: OptInCohort, terms: (TermsType, TermsAccepted), emailOption: Option[String], preferencesStatus: PreferencesStatus)(implicit request: Request[_], hc: HeaderCarrier) =
    auditConnector.sendEvent(ExtendedDataEvent(
      auditSource = appName,
      auditType = if (preferencesStatus == PreferencesFailure) EventTypes.Failed else EventTypes.Succeeded,
      tags = hc.toAuditTags("Set Print Preference", request.path),
      detail = Json.toJson(hc.toAuditDetails(
        "client" -> "YTA",
        "utr" -> utr.toString,
        "journey" -> journey.toString,
        "digital" -> terms._2.accepted.toString,
        "cohort" -> cohort.toString,
        "TandCsScope" -> terms._1.toString.toLowerCase,
        "userConfirmedReadTandCs" -> terms._2.accepted.toString,
        "email" -> emailOption.getOrElse(""),
        "newUserPreferencesCreated" -> (preferencesStatus == PreferencesCreated).toString))))

  protected def _displayNearlyDone(emailAddress: Option[Encrypted[EmailAddress]])(implicit authContext: AuthContext, request: Request[AnyRef], hostContext: HostContext): Result = {
    Ok(views.html.account_details_printing_preference_confirm(calculateCohort(authContext), emailAddress.map(_.decryptedValue)))
  }
}

