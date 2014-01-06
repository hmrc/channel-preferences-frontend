package controllers.paye

import controllers.common.BaseController
import controllers.common.actions.Actions
import views.html.paye.paye_home
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors

class PayeHomeController(override val auditConnector: AuditConnector, override val authConnector: AuthConnector)
  extends BaseController with Actions with PayeRegimeRoots {

  def this() = this(Connectors.auditConnector, Connectors.authConnector)

  def redirectToHome = UnauthorisedAction (request => Redirect(routes.PayeHomeController.home()))

  def home = UnauthorisedAction {
    request =>
      Ok(paye_home())
  }
}
