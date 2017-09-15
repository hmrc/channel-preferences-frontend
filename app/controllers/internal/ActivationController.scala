package controllers.internal

import config.Global
import connectors._
import model.Encrypted
import play.api.mvc.Result
import play.api.Play
import Play.current
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.concurrent.Future

import connectors.EntityResolverConnector
import controllers.{Authentication, ExternalUrlPrefixes}
import model.{FormType, HostContext}
import play.api.libs.json.Json
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier

object ActivationController extends ActivationController {

  override val entityResolverConnector: EntityResolverConnector = EntityResolverConnector

  override protected implicit val authConnector: AuthConnector = Global.authConnector

  val hostUrl = ExternalUrlPrefixes.pfUrlPrefix

}

trait ActivationController extends FrontendController with Actions with AppName with Authentication {

  def entityResolverConnector: EntityResolverConnector

  val hostUrl: String


  def preferences() = authenticated.async {
    implicit authContext =>
      implicit request =>
        entityResolverConnector.getPreferences().map {
          case Some(preference) => Ok(Json.toJson(preference))
          case _ => NotFound
        }
  }

  def preferencesStatus(hostContext: HostContext) = authenticated.async {
    implicit authContext =>
      implicit request =>
        _preferencesStatus(hostContext)
  }

  def preferencesStatusBySvc(svc: String, token: String, hostContext: HostContext) = authenticated.async {
    implicit authContext =>
      implicit request =>
        _preferencesStatusMtd(svc, token, hostContext)
  }

  def legacyPreferencesStatus(formType: FormType, taxIdentifier: String, hostContext: HostContext) = authenticated.async {
    implicit authContext =>
      implicit request =>
        _preferencesStatus(hostContext)
  }

  private def _preferencesStatusMtd(svc: String, token: String, hostContext: HostContext)(implicit hc: HeaderCarrier): Future[Result] = Future {
    Play.configuration.getString(s"tokenService.$svc.callbackUrl") match {
      case Some(redirectUrl) => PreconditionFailed(Json.obj("redirectUserTo" -> redirectUrl))
      case _ => NotFound
    }
  }

  private def _preferencesStatus(hostContext: HostContext)(implicit hc: HeaderCarrier): Future[Result] = {

    val terms = hostContext.termsAndConditions.getOrElse("generic")
    entityResolverConnector.getPreferencesStatus(terms) map {
      case Right(PreferenceFound(true, emailPreference)) =>
        Ok(Json.obj(
          "optedIn" -> true,
          "verifiedEmail" -> emailPreference.fold(false)(_.isVerified)
        ))
      case Right(PreferenceFound(false, _)) =>
        Ok(Json.obj(
          "optedIn" -> false
        ))
      case Right(PreferenceNotFound(Some(email))) if (hostContext.email.exists(_ !=  email.email)) =>
        Conflict
      case Right(PreferenceNotFound(email)) =>
        val encryptedEmail = email.map(e => Encrypted(EmailAddress(e.email)))
        val redirectUrl = hostUrl + controllers.internal.routes.ChoosePaperlessController.redirectToDisplayFormWithCohort(encryptedEmail, hostContext).url
        PreconditionFailed(Json.obj("redirectUserTo" -> redirectUrl))
      case Left(status) => Status(status)
    }
  }
}
