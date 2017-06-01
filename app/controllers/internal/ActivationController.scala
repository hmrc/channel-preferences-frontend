package controllers.internal

import connectors._
import controllers.{Authentication, ExternalUrlPrefixes}
import model.{FormType, HostContext}
import play.api.libs.json.Json
import play.api.mvc.Result
import service._
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.frontend.{auth, controller}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

object ActivationController extends ServiceActivationController {

  val entityResolverConnector: EntityResolverConnector = EntityResolverConnector

  val hostUrl = ExternalUrlPrefixes.pfUrlPrefix

  val authConnector: auth.connectors.AuthConnector = AuthConnector

  val activationService: PaperlessActivateService = PaperlessActivateService
}

trait ActivationController extends controller.FrontendController with Actions with AppName with Authentication {

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

  def _preferencesStatus(hostContext: HostContext)(implicit hc: HeaderCarrier): Future[Result] = {

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


trait ServiceActivationController extends ActivationController with FrontendController with Actions with AppName with Authentication {

  def activationService: PaperlessActivateService

  def paperlessStatusFor(hostContext: HostContext, service: String) = authenticated.async {
    implicit authContext =>
      implicit request => {
        checkPreferenceActivationFlows(hostContext, service)
      }
  }

  def checkPreferenceActivationFlows(hostContext: HostContext, service: String)(implicit headerCarrier: HeaderCarrier) = {

    def preferenceFlow = () => activationService.paperlessPreference(hostContext, service).map {
      case UnAuthorised => Unauthorized
      case RedirectToOptInPage(service, url) =>
        PreconditionFailed(Json.obj("redirectUserTo" -> (hostUrl + url)))
      case PreferenceFound | UserAutoEnrol(_, _) => Ok(Json.obj())
    }

    val entityResolverFlow = () => _preferencesStatus(hostContext)

    val activationFlows: Seq[() => Future[Result]] = Seq(
      entityResolverFlow,
      preferenceFlow // DC-970 calls directly preference
    )

    def conditionalFold(seq: Seq[() => Future[Result]], keepFolding: Result => Boolean): Future[Result] = {
      seq match {
        case head :: Nil => head()
        case head :: tail => {
          head().flatMap {
            case result if keepFolding(result) => conditionalFold(tail, keepFolding)
            case result => conditionalFold(Seq(() => Future.successful(result)), keepFolding)
          }
        }
      }
    }

    val continueIfPreconditionFailed: Result => Boolean = result => result.header.status == PRECONDITION_FAILED

    conditionalFold(activationFlows, continueIfPreconditionFailed)
  }
}