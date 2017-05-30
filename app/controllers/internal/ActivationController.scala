package controllers.internal

import connectors._
import controllers.{Authentication, ExternalUrlPrefixes}
import model.{FormType, HostContext}
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.auth
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.collection.Map
import scala.concurrent.Future

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
    implicit authContext =>
      implicit request => {

        for {
          taxIds <- authorityConnector.currentTaxIdentifiers
          preferences <- Future.traverse(taxIds.toSeq) {
            taxId =>
              preferenceConnector.getPreferencesStatus(taxId.name, taxId.value)
                .map(p => (taxId.name -> p.map(_.services)))
          }
        } yield {
          val test = for {
            (taxId, maybePreference) <- preferences
            preference <- maybePreference
            servicePreference <- preference.get(service)
          } yield (taxId, servicePreference)

          val servicesMap = test.toMap

          if (servicesMap.isEmpty) {
            PreconditionFailed(Json.obj(
              "redirectUserTo" -> (hostUrl + routes.ChoosePaperlessController.redirectToDisplayServiceFormWithCohort(None, hostContext, service).url)
            ))
          } else {
            Ok((Json.toJson(servicesMap)))
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
