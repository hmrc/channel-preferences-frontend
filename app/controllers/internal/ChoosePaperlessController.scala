package controllers.internal

import config.Global
import connectors._
import controllers.internal.EmailOptInJourney._
import controllers.internal.OptInDetailsForm.Data.PaperlessChoice.OptedIn
import controllers.{Authentication, FindTaxIdentifier, internal}
import model.{Encrypted, HostContext}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
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

object ChoosePaperlessController extends ChoosePaperlessController {

  val auditConnector = Global.auditConnector
  val entityResolverConnector = EntityResolverConnector
  val emailConnector = EmailConnector

  override protected implicit def authConnector: AuthConnector = Global.authConnector
}

trait ChoosePaperlessController extends FrontendController with OptInCohortCalculator with Authentication with Actions with AppName with FindTaxIdentifier {

  def entityResolverConnector: EntityResolverConnector
  def emailConnector: EmailConnector
  def auditConnector: AuditConnector

  def redirectToDisplayFormWithCohort(emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = authenticated { implicit authContext => implicit request =>
    createRedirectToDisplayFormWithCohort(emailAddress, hostContext)
  }

  private def createRedirectToDisplayFormWithCohort(emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext)(implicit authContext: AuthContext) =
    Redirect(routes.ChoosePaperlessController.displayForm(Some(calculateCohort(hostContext)), emailAddress, hostContext))

  def displayForm(implicit cohort: Option[OptInCohort], emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = authenticated.async { implicit authContext => implicit request =>
    cohort.fold(ifEmpty = Future.successful(createRedirectToDisplayFormWithCohort(emailAddress, hostContext))) { cohort => {
      auditPageShown(authContext, AccountDetails, cohort)
      val email = emailAddress.map(_.decryptedValue)

      def form(emailAlreadyStored: Boolean): Form[_] =
        if (hostContext.termsAndConditions.contains("taxCredits")) {
          OptInTaxCreditsDetailsForm().fill(OptInTaxCreditsDetailsForm.Data(emailAddress = email, termsAndConditions = (None, None), emailAlreadyStored = Some(emailAlreadyStored)))
        } else {
          OptInDetailsForm().fill(OptInDetailsForm.Data(emailAddress = email, preference = None, acceptedTcs = None, emailAlreadyStored = Some(emailAlreadyStored)))
        }


      hasStoredEmail(hostContext).map(emailAlreadyStored =>
        Ok(views.html.sa.prefs.sa_printing_preference(
          emailForm = form(emailAlreadyStored),
          submitPrefsFormAction = internal.routes.ChoosePaperlessController.submitForm(hostContext),
          cohort = cohort
        )))
    }}}


  def submitForm(implicit hostContext: HostContext) = authenticated.async { implicit authContext => implicit request =>
    val cohort = calculateCohort(hostContext)

    def saveAndAuditPreferences(digital: Boolean, email: Option[String], termsType: TermsType, emailAlreadyStored: Boolean)(implicit hc: HeaderCarrier): Future[Result] = {
      val terms = termsType -> TermsAccepted(digital)

      entityResolverConnector.updateTermsAndConditions(terms, email).map( preferencesStatus => {
        auditChoice(authContext, AccountDetails, cohort, terms, email, preferencesStatus)
        if (digital && !emailAlreadyStored) {
          val encryptedEmail = email map (emailAddress => Encrypted(EmailAddress(emailAddress)))
          Redirect(routes.ChoosePaperlessController.displayNearlyDone(encryptedEmail, hostContext))
        } else Redirect(hostContext.returnUrl)
      })
    }

    def returnToFormWithErrors(f: Form[_]) = {
      Future.successful(BadRequest(views.html.sa.prefs.sa_printing_preference(f, routes.ChoosePaperlessController.submitForm(hostContext), cohort)))
    }

    def handleTc(): Future[Result] = {
      OptInOrOutTaxCreditsForm().bindFromRequest.fold[Future[Result]](
        hasErrors = returnToFormWithErrors,
        happyForm =>
          if (happyForm.optedIn.contains(false)) saveAndAuditPreferences(digital = false, email = None, cohort.terms, false)
          else OptInTaxCreditsDetailsForm().bindFromRequest.fold[Future[Result]](
            hasErrors = returnToFormWithErrors,
            success = {
              case emailForm@OptInTaxCreditsDetailsForm.Data((Some(emailAddress), _),_ , _, (Some(true), Some(true))) =>
                val emailVerificationStatus =
                  if (emailForm.isEmailVerified) Future.successful(true)
                  else emailConnector.isValid(emailAddress)

                emailVerificationStatus.flatMap {
                  case true => saveAndAuditPreferences(digital = true, email = Some(emailAddress), cohort.terms, emailForm.isEmailAlreadyStored)
                  case false =>
                    Future.successful(Ok(views.html.sa_printing_preference_verify_email(emailAddress, cohort)))
                }
              case _ =>
                returnToFormWithErrors(OptInDetailsForm().bindFromRequest)
            }
          )
      )
    }

    def handleGeneric(): Future[Result] = {
      OptInOrOutForm().bindFromRequest.fold[Future[Result]](
        hasErrors = returnToFormWithErrors,
        happyForm =>
          if (happyForm.optedIn.contains(false)) saveAndAuditPreferences(digital = false, email = None, cohort.terms, false)
          else OptInDetailsForm().bindFromRequest.fold[Future[Result]](
            hasErrors = returnToFormWithErrors,
            success = {
              case emailForm@OptInDetailsForm.Data((Some(emailAddress), _),_, Some(OptedIn), Some(true), _) =>
                val emailVerificationStatus =
                  if (emailForm.isEmailVerified) Future.successful(true)
                  else emailConnector.isValid(emailAddress)

                emailVerificationStatus.flatMap {
                  case true => saveAndAuditPreferences(digital = true, email = Some(emailAddress), cohort.terms, emailForm.isEmailAlreadyStored)
                  case false =>
                    Future.successful(Ok(views.html.sa_printing_preference_verify_email(emailAddress, cohort)))
                }
              case _ =>
                returnToFormWithErrors(OptInDetailsForm().bindFromRequest)
            }
          )
      )
    }

    if (hostContext.termsAndConditions.contains("taxCredits")) handleTc()
    else handleGeneric()
  }

  private def auditPageShown(authContext: AuthContext, journey: Journey, cohort: OptInCohort)(implicit request: Request[_], hc: HeaderCarrier) =
    auditConnector.sendEvent(ExtendedDataEvent(
      auditSource = appName,
      auditType = EventTypes.Succeeded,
      tags = hc.toAuditTags("Show Print Preference Option", request.path),
      detail = Json.toJson(hc.toAuditDetails(
        "utr" -> findUtr(authContext).map(_.utr).getOrElse("N/A"),
        "nino" -> findNino(authContext).map(_.nino).getOrElse("N/A"),
        "journey" -> journey.toString,
        "cohort" -> cohort.toString
      ))
    ))

  private def auditChoice(authContext: AuthContext, journey: Journey, cohort: OptInCohort, terms: (TermsType, TermsAccepted), emailOption: Option[String], preferencesStatus: PreferencesStatus)(implicit request: Request[_], message: play.api.i18n.Messages, hc: HeaderCarrier) =
    auditConnector.sendEvent(ExtendedDataEvent(
      auditSource = appName,
      auditType = if (preferencesStatus == PreferencesFailure) EventTypes.Failed else EventTypes.Succeeded,
      tags = hc.toAuditTags("Set Print Preference", request.path),
      detail = Json.toJson(hc.toAuditDetails(
        "client" -> "YTA",
        "utr" -> findUtr(authContext).map(_.utr).getOrElse("N/A"),
        "nino" -> findNino(authContext).map(_.nino).getOrElse("N/A"),
        "journey" -> journey.toString,
        "digital" -> terms._2.accepted.toString,
        "cohort" -> cohort.toString,
        "TandCsScope" -> terms._1.toString.toLowerCase,
        "userConfirmedReadTandCs" -> terms._2.accepted.toString,
        "email" -> emailOption.getOrElse(""),
        "newUserPreferencesCreated" -> (preferencesStatus == PreferencesCreated).toString
      ))
    ))

  private def hasStoredEmail(hostContext: HostContext)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val terms = hostContext.termsAndConditions.getOrElse("generic")
    entityResolverConnector.getPreferencesStatus(terms) map {
      case Right(PreferenceNotFound(Some(_))) | Right(PreferenceFound(false, Some(_))) => true
      case _ => false
    }
  }

  def displayNearlyDone(emailAddress: Option[Encrypted[EmailAddress]], hostContext: HostContext) = authenticated { implicit authContext => implicit request =>
    implicit val hostContextImplicit = hostContext
    Ok(views.html.account_details_printing_preference_confirm(calculateCohort(hostContext), emailAddress.map(_.decryptedValue)))
  }
}

