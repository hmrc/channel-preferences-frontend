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

object ChoosePaperlessController extends ChoosePaperlessController with Authentication with Actions {

  val auditConnector = Global.auditConnector
  val preferencesConnector = EntityResolverConnector
  val emailConnector = EmailConnector
  protected implicit def authConnector: AuthConnector = Global.authConnector
}

trait ChoosePaperlessController extends FrontendController with OptInCohortCalculator with Authentication with Actions with AppName {

  def preferencesConnector: EntityResolverConnector
  def emailConnector: EmailConnector
  def auditConnector: AuditConnector

  def calculateCohort(authContext: AuthContext): OptInCohort

  def redirectToDisplayFormWithCohort(emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = authenticated { implicit authContext => implicit request =>
    createRedirectToDisplayFormWithCohort(emailAddress, hostContext)
  }

  private def createRedirectToDisplayFormWithCohort(emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext)(implicit authContext: AuthContext) =
    Redirect(routes.ChoosePaperlessController.displayForm(Some(calculateCohort(authContext)), emailAddress, hostContext))

  def displayForm(implicit cohort: Option[OptInCohort], emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = authenticated.async { implicit authContext => implicit request =>
    val saUtr = authContext.principal.accounts.sa.get.utr
    cohort.fold(ifEmpty = Future.successful(createRedirectToDisplayFormWithCohort(emailAddress, hostContext))) { cohort =>
      preferencesConnector.saveCohort(saUtr, calculateCohort(authContext)).map { case _ =>
        auditPageShown(saUtr, AccountDetails, cohort)
        val email = emailAddress.map(_.decryptedValue)
        Ok(views.html.sa.prefs.sa_printing_preference(
          emailForm = OptInDetailsForm().fill(OptInDetailsForm.Data(emailAddress = email, preference = email.map(_ => OptInDetailsForm.Data.PaperlessChoice.OptedIn), acceptedTcs = Some(false))),
          submitPrefsFormAction = internal.routes.ChoosePaperlessController.submitForm(hostContext),
          cohort = cohort
        ))
      }
    }
  }

  def submitForm(implicit hostContext: HostContext) = authenticated.async { implicit authContext => implicit request =>
    val cohort = calculateCohort(authContext)
    val saUtr = authContext.principal.accounts.sa.get.utr

    def saveAndAuditPreferences(utr:SaUtr, digital: Boolean, email: Option[String])(implicit hc: HeaderCarrier): Future[Result] = {
      val terms = Generic -> TermsAccepted(digital)
      for {
        _ <- preferencesConnector.saveCohort(utr, calculateCohort(authContext))
        preferencesStatus <- preferencesConnector.updateTermsAndConditions(utr, terms, email)
      } yield {
        auditChoice(utr, AccountDetails, cohort, terms, email, preferencesStatus)
        digital match {
          case true  =>
            val encryptedEmail = email map (emailAddress => Encrypted(EmailAddress(emailAddress)))
            Redirect(routes.ChoosePaperlessController.displayNearlyDone(encryptedEmail, hostContext))
          case false => Redirect(hostContext.returnUrl)
        }
      }
    }

    def returnToFormWithErrors(f: Form[_]) = Future.successful(BadRequest(views.html.sa.prefs.sa_printing_preference(f, routes.ChoosePaperlessController.submitForm(hostContext), cohort)))

    OptInOrOutForm().bindFromRequest.fold[Future[Result]](
      hasErrors = returnToFormWithErrors,
      happyForm =>
        if (happyForm.optedIn.contains(false)) saveAndAuditPreferences(saUtr, digital = false, email = None)
        else OptInDetailsForm().bindFromRequest.fold[Future[Result]](
          hasErrors = returnToFormWithErrors,
          success = {
            case emailForm@OptInDetailsForm.Data((Some(emailAddress), _), _, Some(OptInDetailsForm.Data.PaperlessChoice.OptedIn), Some(true)) =>
              val emailVerificationStatus =
                if (emailForm.isEmailVerified) Future.successful(true)
                else emailConnector.isValid(emailAddress)

              emailVerificationStatus.flatMap {
                case true => saveAndAuditPreferences(saUtr, digital = true, email = Some(emailAddress))
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
        "cohort" -> cohort.toString
      ))
    ))

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
        "newUserPreferencesCreated" -> (preferencesStatus == PreferencesCreated).toString
      ))
    ))

  def displayNearlyDone(emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = authenticated { implicit authContext => implicit request =>
    Ok(views.html.account_details_printing_preference_confirm(calculateCohort(authContext), emailAddress.map(_.decryptedValue))(request, hostContext))
  }
}

