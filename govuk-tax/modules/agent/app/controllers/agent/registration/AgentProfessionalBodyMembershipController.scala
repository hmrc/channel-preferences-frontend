package controllers.agent.registration

import controllers.common.{ ActionWrappers, SessionTimeoutWrapper, BaseController }
import play.api.data.Form
import play.api.data.Forms._
import scala.Some
import play.api.mvc.Result
import uk.gov.hmrc.microservice.paye.domain.PayeRegime
import controllers.agent.registration.FormNames._

class AgentProfessionalBodyMembershipController extends BaseController with SessionTimeoutWrapper with ActionWrappers with AgentMapper with MultiformRegistration {
  private val professionalBodyMembershipForm: Form[AgentProfessionalBodyMembership] = Form(
    mapping(
      AgentProfessionalBodyMembershipFormFields.professionalBodyMembership -> tuple(
        AgentProfessionalBodyMembershipFormFields.professionalBody -> optional(text.verifying("error.illegal.value", v => { Configuration.config.professionalBodyOptions.contains(v) })),
        AgentProfessionalBodyMembershipFormFields.membershipNumber -> optional(text)
      ).verifying("error.agent.professionalBodyMembershipNumber.mandatory", data => (!data._1.isDefined || data._2.isDefined))
        .verifying("error.agent.professionalBodyMembership.mandatory", data => (data._1.isDefined || !data._2.isDefined))
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
                saveFormToKeyStore(professionalBodyMembershipFormName, agentProfessionalBodyMembership, userId(user))
                val keyStore = getKeyStore(userId(user))
                keyStore match {
                  case Some(x) => {
                    val agentId = agentMicroService.create(toAgent(x)).get.uar.getOrElse("")
                    deleteFromKeyStore(userId(user))
                    Ok(agentId)
                  }
                  case _ => Redirect(routes.AgentContactDetailsController.contactDetails())
                }

              }
            )
      }
    }

}

case class AgentProfessionalBodyMembership(professionalBody: Option[String] = None, membershipNumber: Option[String] = None)

object AgentProfessionalBodyMembershipFormFields {
  val professionalBodyMembership = "professionalBodyMembership"
  val professionalBody = "professionalBody"
  val membershipNumber = "membershipNumber"
  val qualifiedProfessionalBody = professionalBodyMembership + "." + professionalBody
  val qualifiedMembershipNumber = professionalBodyMembership + "." + membershipNumber
}