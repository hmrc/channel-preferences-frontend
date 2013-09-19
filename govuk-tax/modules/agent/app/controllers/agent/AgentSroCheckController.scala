package controllers.agent

import play.api.data._
import play.api.data.Forms._
import controllers.common._
import play.api.Logger

class AgentSroCheckController
    extends BaseController
    with SessionTimeoutWrapper
    with ActionWrappers {

  def reasonForApplication() = UnauthorisedAction {
    implicit request =>
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

  def submitAgreement = UnauthorisedAction {
    implicit request =>
      userForm.bindFromRequest.fold(
        errors => {
          BadRequest(views.html.agents.sro_check(errors))
        },
        _ => {
          Logger.debug(s"Redirecting to contact details. Session is $session")
          import controllers.agent.registration.routes
          Redirect(routes.AgentContactDetailsController.contactDetails)
        }
      )
  }

}

case class SroCheck(sroAgreement: Boolean = false, tncAgreement: Boolean = false)

