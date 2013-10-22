package controllers.agent.registration

import controllers.common.{ActionWrappers, BaseController}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{SimpleResult, Request}
import controllers.agent.registration.FormNames._
import controllers.common.actions.MultiFormWrapper
import service.agent.AgentMicroServices

class AgentThankYouController extends BaseController with ActionWrappers with AgentController
  with MultiFormWrapper with AgentMapper with AgentMicroServices {

  def thankYou = AuthorisedForIdaAction(Some(PayeRegime)) {
    MultiFormAction(multiFormConfig) {
      user => request => thankYouAction(user, request)
    }
  }

  private[registration] val thankYouAction: ((User, Request[_]) => SimpleResult) = (user, request) => {
    keyStoreMicroService.getKeyStore[Map[String, String]](registrationId(user), agent) match {
      case Some(x) => {
        val agentId = agentMicroService.create(user.regimes.paye.get.nino, toAgent(x)).uar

        keyStoreMicroService.deleteKeyStore(registrationId(user), agent)
        Ok(views.html.agents.registration.thank_you_page(agentId))
      }
      case _ => Redirect(routes.AgentContactDetailsController.contactDetails())
    }
  }

  def step: String = thankYouName
}
