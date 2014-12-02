package controllers.sa.prefs.internal

import controllers.common.BaseController
import controllers.common.actions.Actions
import play.api.mvc.Action
import uk.gov.hmrc.common.microservice.auth.AuthConnector

import scala.concurrent.Future

class BizTaxHomePagePartialController(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions {

  def preferencesWarning() = Action.async { implicit request =>
    Future.successful(Ok)
  }
}
