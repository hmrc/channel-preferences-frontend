package controllers.agent.registration

import play.api.data.Form
import play.api.data.Forms._
import scala.Some
import play.api.mvc.{ Request, Result }
import uk.gov.hmrc.microservice.paye.domain.PayeRegime
import controllers.agent.registration.FormNames._
import uk.gov.hmrc.microservice.domain.User
import controllers.common.service.MicroServices
import controllers.common.{ ActionWrappers, SessionTimeoutWrapper, BaseController }
import controllers.common.validators.Validators
import controllers.common.actions.MultiFormWrapper

class AgentProfessionalBodyMembershipController extends BaseController with SessionTimeoutWrapper with ActionWrappers with AgentController with MicroServices with Validators with MultiFormWrapper {

  private val professionalBodyMembershipForm = Form[AgentProfessionalBodyMembership](
    mapping(
      AgentProfessionalBodyMembershipFormFields.professionalBodyMembership -> tuple(
        AgentProfessionalBodyMembershipFormFields.professionalBody -> optional(smallText.verifying("error.illegal.value", v => { Configuration.config.professionalBodyOptions.contains(v) })),
        AgentProfessionalBodyMembershipFormFields.membershipNumber -> optional(smallText)
      ).verifying("error.agent.professionalBodyMembershipNumber.mandatory", data => (!data._1.isDefined || data._2.isDefined))
        .verifying("error.agent.professionalBodyMembership.mandatory", data => (data._1.isDefined || !data._2.isDefined))
    ) {
        (professionalBodyMembership) =>
          AgentProfessionalBodyMembership(professionalBodyMembership._1, professionalBodyMembership._2)
      } {
        form => Some((form.professionalBody, form.membershipNumber))
      }
  )

  def professionalBodyMembership = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { MultiFormAction(multiFormConfig(_)) { user => request => professionalBodyMembershipAction(user, request) } } }

  private[registration] val professionalBodyMembershipAction: (User, Request[_]) => Result = (user, request) => {
    val form = professionalBodyMembershipForm.fill(AgentProfessionalBodyMembership())
    Ok(views.html.agents.registration.professional_body_membership(form, Configuration.config.professionalBodyOptions))
  }

  def postProfessionalBodyMembership = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { MultiFormAction(multiFormConfig(_)) { user => request => postProfessionalBodyMembershipAction(user, request) } } }

  private[registration] val postProfessionalBodyMembershipAction: ((User, Request[_]) => Result) = (user, request) => {
    professionalBodyMembershipForm.bindFromRequest()(request).fold(
      errors => {
        BadRequest(views.html.agents.registration.professional_body_membership(errors, Configuration.config.professionalBodyOptions))
      },
      _ => {
        val agentProfessionalBodyMembership = professionalBodyMembershipForm.bindFromRequest()(request).data
        keyStoreMicroService.addKeyStoreEntry(registrationId(user), agent, professionalBodyMembershipFormName, agentProfessionalBodyMembership)
        Redirect(routes.AgentThankYouController.thankYou())
      }
    )
  }

  def step: String = professionalBodyMembershipFormName

}

case class AgentProfessionalBodyMembership(professionalBody: Option[String] = None, membershipNumber: Option[String] = None)

object AgentProfessionalBodyMembershipFormFields {
  val professionalBodyMembership = "professionalBodyMembership"
  val professionalBody = "professionalBody"
  val membershipNumber = "membershipNumber"
  val qualifiedProfessionalBody = professionalBodyMembership + "." + professionalBody
  val qualifiedMembershipNumber = professionalBodyMembership + "." + membershipNumber
}