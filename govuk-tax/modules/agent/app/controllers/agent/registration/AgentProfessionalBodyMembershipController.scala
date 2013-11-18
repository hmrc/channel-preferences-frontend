package controllers.agent.registration

import play.api.data.Form
import play.api.data.Forms._
import scala.Some
import play.api.mvc.{SimpleResult, Request}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import controllers.agent.registration.FormNames._
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.BaseController
import controllers.common.validators.Validators
import controllers.common.actions.{Actions, MultiFormWrapper}
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors

class AgentProfessionalBodyMembershipController(override val auditConnector: AuditConnector,
                                                override val keyStoreConnector: KeyStoreConnector)
                                               (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with AgentController
  with Validators
  with MultiFormWrapper {

  def this() = this(Connectors.auditConnector, Connectors.keyStoreConnector)(Connectors.authConnector)

  private val professionalBodyMembershipForm = Form[AgentProfessionalBodyMembership](
    mapping(
      AgentProfessionalBodyMembershipFormFields.professionalBodyMembership -> tuple(
        AgentProfessionalBodyMembershipFormFields.professionalBody -> optional(smallText.verifying("error.illegal.value", v => {
          Configuration.config.professionalBodyOptions.exists(_._1 == v)
        })),
        AgentProfessionalBodyMembershipFormFields.membershipNumber -> optional(smallText)
      ).verifying("error.agent.professionalBodyMembershipNumber.mandatory", data => !data._1.isDefined || (data._2.isDefined && notBlank(data._2.getOrElse(""))))
        .verifying("error.agent.professionalBodyMembership.mandatory", data => data._1.isDefined || !data._2.isDefined)
    ) {
      (professionalBodyMembership) =>
        AgentProfessionalBodyMembership(professionalBodyMembership._1, professionalBodyMembership._2)
    } {
      form => Some((form.professionalBody, form.membershipNumber))
    }
  )

  def professionalBodyMembership = AuthorisedFor(PayeRegime) {
    MultiFormAction(multiFormConfig) {
      user => request => professionalBodyMembershipAction(user, request)
    }
  }

  def postProfessionalBodyMembership = AuthorisedFor(PayeRegime) {
    MultiFormAction(multiFormConfig) {
      user => request => postProfessionalBodyMembershipAction(user, request)
    }
  }

  private[registration] val professionalBodyMembershipAction: (User, Request[_]) => SimpleResult = (user, request) => {
    val form = professionalBodyMembershipForm.fill(AgentProfessionalBodyMembership())
    Ok(views.html.agents.registration.professional_body_membership(form, Configuration.config.professionalBodyOptions))
  }

  private[registration] val postProfessionalBodyMembershipAction: ((User, Request[_]) => SimpleResult) = (user, request) => {
    professionalBodyMembershipForm.bindFromRequest()(request).fold(
      errors => {
        BadRequest(views.html.agents.registration.professional_body_membership(errors, Configuration.config.professionalBodyOptions))
      },
      _ => {
        val agentProfessionalBodyMembership = professionalBodyMembershipForm.bindFromRequest()(request).data
        keyStoreConnector.addKeyStoreEntry(registrationId(user), agent, professionalBodyMembershipFormName, agentProfessionalBodyMembership)
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