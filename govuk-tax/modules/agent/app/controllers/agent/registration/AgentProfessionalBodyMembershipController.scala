package controllers.agent.registration

import controllers.common.{ ActionWrappers, SessionTimeoutWrapper, BaseController }
import play.api.data.Form
import play.api.data.Forms._
import scala.Some
import play.api.mvc.Result
import uk.gov.hmrc.microservice.paye.domain.PayeRegime

class AgentProfessionalBodyMembershipController extends BaseController with SessionTimeoutWrapper with ActionWrappers with MultiformRegistration {
  private val professionalBodyMembershipForm: Form[AgentProfessionalBodyMembership] = Form(
    mapping(
      "professionalBodyMembership" -> tuple(
        "professionalBody" -> optional(text),
        "membershipNumber" -> optional(text)
      ).verifying("error.agent.professionalBodyMembership.mandatory.details", data => (data._1.isDefined && data._2.isDefined) || (!data._1.isDefined))
    ) {
        (professionalBodyMembership) =>
          AgentProfessionalBodyMembership(professionalBodyMembership._1, professionalBodyMembership._2)
      } {
        form => Some((form.professionalBody, form.membershipNumber))
      }
  )

  def professionalBodyMembership =
    AuthorisedForIdaAction(Some(PayeRegime)) {
      user =>
        request => {
          professionalBodyMembershipFunction
        }
    }

  val professionalBodyMembershipFunction: Result = {
    val form = professionalBodyMembershipForm.fill(AgentProfessionalBodyMembership())
    Ok(views.html.agents.registration.professional_body_membership(form, Configuration.config.professionalBodyOptions))
  }

  def postProfessionalBodyMembership =
    WithSessionTimeoutValidation {
      AuthorisedForIdaAction(Some(PayeRegime)) {
        user =>
          implicit request =>
            professionalBodyMembershipForm.bindFromRequest.fold(
              errors => {
                println(errors)
                BadRequest(views.html.agents.registration.professional_body_membership(errors, Configuration.config.professionalBodyOptions))
              },
              _ => {
                val agentProfessionalBodyMembership = professionalBodyMembershipForm.bindFromRequest.data
                saveFormToKeyStore("professionalBodyMembershipForm", agentProfessionalBodyMembership, userId(user))
                //Save agent!!!
                Ok("Thank you!")

              }
            )
      }
    }

}

case class AgentProfessionalBodyMembership(professionalBody: Option[String] = None, membershipNumber: Option[String] = None)
