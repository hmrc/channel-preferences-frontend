package controllers.internal

import connectors._
import controllers.{Authentication, ExternalUrlPrefixes}
import model.{FormType, HostContext}
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.domain.TaxIds.TaxIdWithName
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.auth
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

object ActivationController extends ActivationController with ServiceActivationController {

  val entityResolverConnector: EntityResolverConnector = EntityResolverConnector

  val preferenceConnector: PreferencesConnector = PreferencesConnector

  val authorityConnector: AuthConnector = AuthConnector

  val hostUrl = ExternalUrlPrefixes.pfUrlPrefix

  override protected val authConnector: auth.connectors.AuthConnector = AuthConnector
}

trait ServiceActivationController extends FrontendController with Actions with AppName with Authentication {

  val hostUrl: String

  def authorityConnector: AuthConnector

  def preferenceConnector: PreferencesConnector

  def paperlessPreference(hostContext: HostContext, service: String) = authenticated.async {
    import PaperlessPreference.formats
    implicit authContext =>
      implicit request => {
        for {
          taxIds <- authorityConnector.currentTaxIdentifiers
          preferences <- Future.traverse(taxIds) { taxId => preferenceConnector.getPreferencesStatus(taxId.name, taxId.value) }
        } yield {
          preferences.toSeq.flatten match {
            case Seq() => PreconditionFailed(Json.obj(
              "redirectUserTo" -> (hostUrl + routes.ChoosePaperlessController.redirectToDisplayServiceFormWithCohort(None, hostContext, service).url)
            ))
            case preferences => Ok((Json.toJson(preferences)))
          }
        }
      }
  }
}


trait ActivationController extends FrontendController with Actions with AppName with Authentication {

  def entityResolverConnector: EntityResolverConnector

  val hostUrl: String

  def preferencesStatus(hostContext: HostContext) = authenticated.async {
    implicit authContext =>
      implicit request =>
        _preferencesStatus(hostContext)
  }

  def legacyPreferencesStatus(formType: FormType, taxIdentifier: String, hostContext: HostContext) = authenticated.async {
    implicit authContext =>
      implicit request =>
        _preferencesStatus(hostContext)
  }

  private def _preferencesStatus(hostContext: HostContext)(implicit hc: HeaderCarrier): Future[Result] = {

    def isEmailVerified(emailPreference: Option[SaEmailPreference]) = {
      emailPreference.fold(false)(preference => (preference.status match {
        case SaEmailPreference.Status.Verified => true
        case _ => false
      }))
    }

    entityResolverConnector.getPreferencesStatus().map {
      case Right(SaPreference(true, emailPreference)) => Ok(Json.obj(
        "optedIn" -> true,
        "verifiedEmail" -> isEmailVerified(emailPreference)
      ))
      case Right(SaPreference(false, _)) => Ok(Json.obj(
        "optedIn" -> false
      ))
      case Left(412) =>
        val redirectUrl = hostUrl + controllers.internal.routes.ChoosePaperlessController.redirectToDisplayFormWithCohort(None, hostContext).url
        PreconditionFailed(Json.obj("redirectUserTo" -> redirectUrl))
      case Left(status) => Status(status)
    }
  }
}
