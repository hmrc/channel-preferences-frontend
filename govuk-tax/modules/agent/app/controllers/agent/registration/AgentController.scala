package controllers.agent.registration

import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.actions.{ MultiFormStep, MultiFormConfiguration }

trait AgentController {

  def registrationId(user: User) = "Registration:" + user.oid

  def uar(user: User) = "UAR:" + user.oid

  def multiFormConfig(user: User) = MultiFormConfiguration(
    id = registrationId(user),
    source = agent,
    stepsList = FormNames.stepsOrder,
    currentStep = step,
    unauthorisedStep = FormNames.stepsOrder.head)

  def step: String

  val agent = "agent"
  val uar = "uar"
}

object FormNames {

  val professionalBodyMembershipFormName = "professionalBodyMembershipForm"
  val companyDetailsFormName = "companyDetailsForm"
  val agentTypeAndLegalEntityFormName = "agentTypeAndLegalEntityForm"
  val contactFormName = "contactForm"
  val thankYouName = "thankYou"

  val stepsOrder: List[MultiFormStep] = List(
    MultiFormStep(contactFormName, routes.AgentContactDetailsController.contactDetails()),
    MultiFormStep(agentTypeAndLegalEntityFormName, routes.AgentTypeAndLegalEntityController.agentType()),
    MultiFormStep(companyDetailsFormName, routes.AgentCompanyDetailsController.companyDetails()),
    MultiFormStep(professionalBodyMembershipFormName, routes.AgentProfessionalBodyMembershipController.professionalBodyMembership()),
    MultiFormStep(thankYouName, routes.AgentThankYouController.thankYou())
  )

}
