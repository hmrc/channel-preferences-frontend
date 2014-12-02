package controllers.sa.prefs.internal

import connectors.PreferencesConnector
import controllers.common.BaseController
import controllers.common.actions.Actions
import controllers.common.service.Connectors
import play.api.mvc.Action
import uk.gov.hmrc.common.microservice.auth.AuthConnector

import scala.concurrent.Future

class BizTaxHomePagePartialController(val preferenceConnector: PreferencesConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions {

  def this() = this(PreferencesConnector)(Connectors.authConnector)

  def preferencesWarning() = Action.async { implicit request =>
    Future.successful(Ok)
  }
}
