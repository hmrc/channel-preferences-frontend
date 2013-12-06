package controllers.agent

import play.api.data._
import play.api.data.Forms._
import controllers.common._
import play.api.Logger
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.service.Connectors
import controllers.common.actions.Actions
import scala.concurrent.Future
import play.api.mvc.{Request, SimpleResult}

class AgentSroCheckController(override val auditConnector: AuditConnector)
                             (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with AgentsRegimeRoots {

  def this() = this(Connectors.auditConnector)(Connectors.authConnector)

  def reasonForApplication() = UnauthorisedAction {
    request =>
      Ok(views.html.agents.reason_for_application())
  }

  val userForm = Form[SroCheck](
    mapping(
      "sroAgreement" -> checked("error.agent.accept.sroAgreement"),
      "tncAgreement" -> checked("error.agent.accept.tncAgreement")
    )(SroCheck.apply)(SroCheck.unapply)
  )

  def sroCheck() = UnauthorisedAction {
    implicit request =>
      Ok(views.html.agents.sro_check(userForm))
  }

  def submitAgreement  = UnauthorisedAction {
    implicit request =>
      userForm.bindFromRequest.fold(
        errors =>
          BadRequest(views.html.agents.sro_check(errors)),
        _ => {
          Logger.debug(s"Redirecting to contact details. Session is $session")
          import controllers.agent.registration.routes
          Redirect(routes.AgentContactDetailsController.contactDetails)
        }
      )
  }

}

case class SroCheck(sroAgreement: Boolean = false, tncAgreement: Boolean = false)

