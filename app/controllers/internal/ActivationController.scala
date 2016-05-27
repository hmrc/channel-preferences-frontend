package controllers.internal

import config.Global
import connectors.EntityResolverConnector
import controllers.{Authentication, ExternalUrlPrefixes}
import model.{Encrypted, FormType, HostContext}
import play.api.libs.json.Json
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

object ActivationController extends ActivationController {

  override val entityResolverConnector: EntityResolverConnector = EntityResolverConnector

  override protected implicit val authConnector: AuthConnector = Global.authConnector

  val hostUrl = ExternalUrlPrefixes.pfUrlPrefix
}

trait ActivationController extends FrontendController with Actions with AppName with Authentication {

  def entityResolverConnector: EntityResolverConnector

  val hostUrl: String

  def preferencesStatus(formType: FormType, taxIdentifier: String, hostContext: HostContext) = authenticated.async {
    implicit authContext => implicit request =>
      entityResolverConnector.getPreferencesStatus() map { status =>
        status match {
          case 412  => {
            val redirectUrl = hostUrl + controllers.internal.routes.ChoosePaperlessController.redirectToDisplayFormWithCohort(None, hostContext).url
            PreconditionFailed(Json.obj("redirectUserTo" -> redirectUrl))
          }
          case _ => Status(status)
        }
      }
  }
}
