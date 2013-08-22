package controllers.agent

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints.nonEmpty
import controllers.common._
import uk.gov.hmrc.microservice.paye.domain.{ PayeRoot, PayeRegime }
import play.api.Logger
import play.api.mvc.{ Result, Request }
import uk.gov.hmrc.microservice.domain.User
import controllers.common.service.MicroServices
import java.util.regex.Pattern

class AgentController extends BaseController with SessionTimeoutWrapper with ActionWrappers {

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
          Logger.debug(s"Redirecting to contact details. Session is $session")
          Redirect(routes.AgentController.contactDetails).withSession(session + ("register agent" -> "true"))
        }
      )
  }

  def contactDetails =
    AuthorisedForIdaAction(Some(PayeRegime)) {
      user =>
        request => {
          contactDetailsFunction(user, request)
        }
    }

  val contactDetailsFunction: (User, Request[_]) => Result = (user, request) => {
    val paye: PayeRoot = user.regimes.paye.get
    val form = contactForm.fill(AgentDetails())
    Ok(views.html.agents.contact_details(form, paye))
  }

  def postContacts = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(Some(PayeRegime)) {
      user =>
        implicit request =>
          contactForm.bindFromRequest.fold(
            errors => {
              BadRequest(views.html.agents.contact_details(errors, user.regimes.paye.get))
            },
            _ => {
              val agentDetails = contactForm.bindFromRequest.data
              keyStoreMicroService.addKeyStoreEntry("Registration:" + session.get("PLAY_SESSION"), "agent", "contactForm", agentDetails)
              Redirect(routes.AgentController.agentType)
            }
          )

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
      "daytimePhoneNumber" -> text.verifying("error.agent.phone", s => s.matches("\\d+")),
      "mobilePhoneNumber" -> text.verifying("error.agent.phone", s => s.matches("\\d+")),
      "emailAddress" -> email.verifying(nonEmpty)
    )(AgentDetails.apply)(AgentDetails.unapply)
  )
}

case class SroCheck(sroAgreement: Boolean = false, tncAgreement: Boolean = false)
case class AgentDetails(daytimePhoneNumber: String = "", mobilePhoneNumber: String = "", emailAddress: String = "") {

}

