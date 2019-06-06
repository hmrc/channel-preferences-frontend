package controllers.internal

import connectors.{EntityResolverConnector, _}
import controllers.ExternalUrlPrefixes
import controllers.auth.{AuthAction, AuthController}
import model.{Encrypted, FormType, HostContext}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.{Configuration, Play}
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object ActivationController extends ActivationController {

  override val entityResolverConnector: EntityResolverConnector = EntityResolverConnector

  val hostUrl = ExternalUrlPrefixes.pfUrlPrefix

  override protected def appNameConfiguration: Configuration = Play.current.configuration
}

trait ActivationController extends FrontendController with AppName {

  def authorise: AuthAction = AuthController

  def entityResolverConnector: EntityResolverConnector

  val hostUrl: String

  def preferences() = authorise.async {
    implicit request =>
      entityResolverConnector.getPreferences().map {
        case Some(preference) => Ok(Json.toJson(preference))
        case _ => NotFound
      }
  }

  def preferencesStatus(hostContext: HostContext) = authorise.async {
    implicit request =>
      _preferencesStatus(hostContext)
  }

  def preferencesStatusBySvc(svc: String, token: String, hostContext: HostContext) = authorise.async {
    implicit request =>
      _preferencesStatusMtd(svc, token, hostContext)
  }

  def legacyPreferencesStatus(formType: FormType, taxIdentifier: String, hostContext: HostContext) = authorise.async {
    implicit request =>
      _preferencesStatus(hostContext)
  }

  private def _preferencesStatusMtd(svc: String, token: String, hostContext: HostContext)(implicit hc: HeaderCarrier): Future[Result] = {
    entityResolverConnector.getPreferencesStatusByToken(svc, token) map {
      case Right(PreferenceNotFound(email)) =>
        val encryptedEmail = email.map(e => Encrypted(EmailAddress(e.email)))
        val redirectUrl = hostUrl + routes.ChoosePaperlessController.redirectToDisplayFormWithCohortBySvc(svc, token, encryptedEmail, hostContext).url
        PreconditionFailed(Json.obj("redirectUserTo" -> redirectUrl))
      case Right(PreferenceFound(true, emailPreference)) =>
        Ok(Json.obj(
          "optedIn" -> true,
          "verifiedEmail" -> emailPreference.fold(false)(_.isVerified)
        ))
      case Right(PreferenceFound(false, email)) =>
        val encryptedEmail = email.map(e => Encrypted(EmailAddress(e.email)))
        val redirectUrl = hostUrl + routes.ChoosePaperlessController.redirectToDisplayFormWithCohortBySvc(svc, token, encryptedEmail, hostContext).url
        Ok(Json.obj(
          "optedIn" -> false,
          "redirectUserTo" -> redirectUrl
        ))
      case _ => NotFound
    }
  }

  private def _preferencesStatus(hostContext: HostContext)(implicit hc: HeaderCarrier): Future[Result] = {

    val terms = hostContext.termsAndConditions.getOrElse("generic")
    entityResolverConnector.getPreferencesStatus(terms) map {
      case Right(PreferenceFound(true, emailPreference)) if hostContext.alreadyOptedInUrl.isDefined =>
        Redirect(hostContext.alreadyOptedInUrl.get)
      case Right(PreferenceFound(true, emailPreference)) =>
        Ok(Json.obj(
          "optedIn" -> true,
          "verifiedEmail" -> emailPreference.fold(false)(_.isVerified)
        ))
      case Right(PreferenceFound(false, _)) =>
        Ok(Json.obj(
          "optedIn" -> false
        ))
      case Right(PreferenceNotFound(Some(email))) if (hostContext.email.exists(_ != email.email)) =>
        Conflict
      case Right(PreferenceNotFound(email)) =>
        val encryptedEmail = email.map(e => Encrypted(EmailAddress(e.email)))
        val redirectUrl = hostUrl + routes.ChoosePaperlessController.redirectToDisplayFormWithCohort(encryptedEmail, hostContext).url
        PreconditionFailed(Json.obj("redirectUserTo" -> redirectUrl))
      case Left(status) => Status(status)
    }
  }
}
