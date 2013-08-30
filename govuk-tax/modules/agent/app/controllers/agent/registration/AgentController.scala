package controllers.agent.registration

import uk.gov.hmrc.microservice.domain.User
import controllers.common.actions.MultiFormConfiguration

trait AgentController {

  def userId(user: User) = { user.user.substring(user.user.lastIndexOf("/") + 1) }

  def registrationId(user: User) = "Registration:" + userId(user)

  def uar(user: User) = "UAR:" + userId(user)

  def multiFormConfig(user: User): MultiFormConfiguration = MultiFormConfiguration(registrationId(user), agent, FormNames.stepsOrder, step, routes.AgentContactDetailsController.contactDetails())

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

  val stepsOrder: List[String] = List(contactFormName, agentTypeAndLegalEntityFormName, companyDetailsFormName, professionalBodyMembershipFormName)

}
