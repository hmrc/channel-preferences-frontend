package controllers.internal

import config.Global
import connectors.EntityResolverConnector
import model.{Encrypted, FormType, HostContext}
import play.api.libs.json.{JsString, Json}
import play.api.mvc.Action
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

object ActivationController extends ActivationController {

  override val entityResolverConnector: EntityResolverConnector = EntityResolverConnector

  override protected implicit val authConnector: AuthConnector = Global.authConnector
}

trait ActivationController extends FrontendController with Actions with AppName {

  def entityResolverConnector: EntityResolverConnector

  def activate(formType: FormType, taxIdentifier: String, hostContext: HostContext) = Action.async(parse.json) { implicit request =>
    entityResolverConnector.activate(formType, taxIdentifier, hostContext, request.body) map { result =>

      val redirectUrl: String = result.body match {
        case "OptInRequired" => controllers.internal.routes.ChoosePaperlessController.redirectToDisplayFormWithCohort(None, hostContext).url
        case "UpgradeRequired" => controllers.internal.routes.UpgradeRemindersController.displayUpgradeForm(Encrypted(hostContext.returnUrl)).url
      }
      Status(result.status)(Json.obj("redirectUserTo" -> redirectUrl))
    }
  }
}
