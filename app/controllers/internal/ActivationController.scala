package controllers.internal

import config.Global
import connectors.EntityResolverConnector
import model.{FormType, HostContext}
import play.api.mvc.Action
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

object ActivationController extends ActivationController {

  override val preferenceConnector: EntityResolverConnector = EntityResolverConnector

  override protected implicit val authConnector: AuthConnector = Global.authConnector
}

trait ActivationController extends FrontendController with Actions with AppName {

  def preferenceConnector: EntityResolverConnector

  def activate(formType: FormType, taxIdentifier: String, hostContext: HostContext) = Action.async(parse.json) { implicit request =>
    preferenceConnector.activate(formType, taxIdentifier, hostContext, request.body) map { result =>
      Status(result.status)(result.body)
    }
  }
}
