package controllers.internal

import config.Global
import connectors.{EntityResolverConnector, SaEmailPreference, SaPreference}
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

  def preferencesStatus(hostContext: HostContext) = authenticated.async {
    implicit authContext => implicit request =>
      _preferencesStatus(hostContext)
  }

  def legacyPreferencesStatus(formType: FormType, taxIdentifier: String, hostContext: HostContext) = authenticated.async {
    implicit authContext => implicit request =>
      _preferencesStatus(hostContext)
  }

  private def _preferencesStatus(hostContext: HostContext)(implicit hc: HeaderCarrier) = {

    def isEmailVerified(preference: SaPreference) = {
      preference.email.fold(false)(preference => (preference.status match {
        case SaEmailPreference.Status.Verified => true
        case _ => false
      }))
    }

    entityResolverConnector.getPreferencesStatus() map {
      case Right(b) => Ok(Json.obj(
        "optedIn" -> b.digital,
        "verifiedEmail" -> isEmailVerified(b))
      )
      case Left(412) =>
        val redirectUrl = hostUrl + controllers.internal.routes.ChoosePaperlessController.redirectToDisplayFormWithCohort(None, hostContext).url
        PreconditionFailed(Json.obj("redirectUserTo" -> redirectUrl))
      case Left(status) => Status(status)
    }
  }
}
