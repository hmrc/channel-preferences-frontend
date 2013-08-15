package controllers.agent

import play.api.data._
import play.api.data.Forms._
import controllers.common._
import uk.gov.hmrc.microservice.paye.domain.{ PayeRoot, PayeRegime }
import play.api.Logger

class AgentController extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  def reasonForApplication() = UnauthorisedAction { implicit request =>
    Ok(views.html.agents.reason_for_application())
  }

  val userForm = Form[SroCheck](
    mapping(
      "sroAgreement" -> checked("error.agent.accept.sroAgreement"),
      "tncAgreement" -> checked("error.agent.accept.tncAgreement")
    )(SroCheck.apply)(SroCheck.unapply)
  )

  def sroCheck() = UnauthorisedAction { implicit request =>
    Ok(views.html.agents.sro_check(userForm))
  }

  def submitAgreement = UnauthorisedAction {
    implicit request =>
      userForm.bindFromRequest.fold(
        errors => {
          BadRequest(views.html.agents.sro_check(errors))
        },
        _ => {
          RedirectUtils.toSamlLogin.withSession(session.copy(Map("register agent" -> "true")))
        }
      )
  }

  def contactDetails = WithSessionTimeoutValidation {

    AuthorisedForIdaAction(Some(PayeRegime)) {
      user =>
        request =>
          val paye: PayeRoot = user.regimes.paye.get
          val form = contactForm.fill(AgentDetails(paye.title, paye.firstName, "", paye.surname, paye.dateOfBirth, paye.nino, "", "", ""))
          Ok(views.html.agents.contact_details(form))
    }
  }

  def postContacts = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(Some(PayeRegime)) {
      user =>
        implicit request =>
          val agentDetails = contactForm.bindFromRequest.data
          Logger.warn(s"TODO: Write this to backend systems: $agentDetails")
          Redirect(routes.AgentController.agentType)
    }
  }

  def agentType = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(Some(PayeRegime)) { user =>
      request =>
        Ok("Agent type and legal entity")
    }
  }

  private val contactForm = Form[AgentDetails](
    mapping(
      "title" -> text,
      "firstName" -> text,
      "middleName" -> text,
      "lastName" -> text,
      "dateOfBirth" -> text,
      "nino" -> text,
      "daytimePhoneNumber" -> text,
      "mobilePhoneNumber" -> text,
      "emailAddress" -> email
    )(AgentDetails.apply)(AgentDetails.unapply)
  )

}

case class SroCheck(sroAgreement: Boolean = false, tncAgreement: Boolean = false)
case class AgentDetails(title: String, firstName: String, middleName: String, lastName: String, dateOfBirth: String, nino: String, daytimePhoneNumber: String, mobilePhoneNumber: String, emailAddress: String)

