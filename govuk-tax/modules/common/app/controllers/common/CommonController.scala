package controllers.common

import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.actions.Actions
import uk.gov.hmrc.common.microservice.audit.AuditConnector


class CommonController(override val auditConnector: AuditConnector) (implicit override val authConnector: AuthConnector) extends BaseController with Actions with NoRegimeRoots {

  def this() = this(Connectors.auditConnector)(Connectors.authConnector)

  def cookies = UnauthorisedAction {
    implicit request =>
      Ok(views.html.cookies())
  }

  def termsAndConditions = UnauthorisedAction {
    implicit request =>
      Ok(views.html.t_and_c())
  }

  def privacyPolicy = UnauthorisedAction {
    implicit request =>
      Ok(views.html.privacy_policy())
  }

  def helpPage = UnauthorisedAction {
    implicit request =>
      Ok(views.html.help())
  }

  def landingRedirect  = UnauthorisedAction {
    implicit request =>
      FrontEndRedirect.forSession(session)
  }
}
