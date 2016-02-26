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

import scala.concurrent.Future

object ActivationController extends ActivationController {

  override val entityResolverConnector: EntityResolverConnector = EntityResolverConnector

  override protected implicit val authConnector: AuthConnector = Global.authConnector

  val hostUrl = ExternalUrlPrefixes.pfUrlPrefix
}

trait ActivationController extends FrontendController with Actions with AppName with Authentication  {

  def entityResolverConnector: EntityResolverConnector

  val hostUrl: String

  def activate(formType: FormType, taxIdentifier: String, hostContext: HostContext) = authenticated.async {
    implicit authContext => implicit request =>
      if (authContext.principal.accounts.sa.isDefined) {
        entityResolverConnector.activate(formType, taxIdentifier, hostContext, request.body.asJson.get) map { result =>
          result.status match {
            case 412 => {
              val redirectUrl: String = result.body match {
                case "OptInRequired" => hostUrl + controllers.internal.routes.ChoosePaperlessController.redirectToDisplayFormWithCohort(None, hostContext).url
                case "UpgradeRequired" => hostUrl + controllers.internal.routes.UpgradeRemindersController.displayUpgradeForm(Encrypted(hostContext.returnUrl)).url
              }
              Status(result.status)(Json.obj("redirectUserTo" -> redirectUrl))
            }
            case _ => Status(result.status)
          }
        }
      } else
        Future.successful(NotFound)
  }
}
