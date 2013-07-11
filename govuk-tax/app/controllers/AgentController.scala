package controllers

import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._

class AgentController extends BaseController with ActionWrappers {

  def reasonForApplication() = Action {
    Ok(views.html.agents.reason_for_application())
  }

  val userForm = Form[SroCheck](
    mapping(
      "sroAgreement" -> checked("error.agent.accept.sroAgreement"),
      "tncAgreement" -> checked("error.agent.accept.tncAgreement")
    )(SroCheck.apply)(SroCheck.unapply)
  )

  def sroCheck() = Action {
    Ok(views.html.agents.sro_check(userForm))
  }

  def submitAgreement = Action {
    implicit request =>
      userForm.bindFromRequest.fold(
        errors => {
          BadRequest(views.html.agents.sro_check(errors))
        },
        _ => {
          Redirect(routes.LoginController.samlLogin)
        }
      )
  }
}

case class SroCheck(sroAgreement: Boolean = false, tncAgreement: Boolean = false)

