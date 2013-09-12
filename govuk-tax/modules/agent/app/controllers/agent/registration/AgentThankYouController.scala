package controllers.agent.registration

import controllers.common.{ ActionWrappers, SessionTimeoutWrapper, BaseController }
import uk.gov.hmrc.microservice.paye.domain.PayeRegime
import uk.gov.hmrc.microservice.domain.User
import play.api.mvc.{ Result, Request }
import controllers.agent.registration.FormNames._
import controllers.common.actions.MultiFormWrapper

class AgentThankYouController extends BaseController with SessionTimeoutWrapper with ActionWrappers with AgentController with MultiFormWrapper with AgentMapper {

  def thankYou = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(Some(PayeRegime)) {
      MultiFormAction(multiFormConfig) {
        user => request => thankYouAction(user, request)
      }
    }
  }

  private[registration] val thankYouAction: ((User, Request[_]) => Result) = (user, request) => {
    keyStoreMicroService.getKeyStore[Map[String, String]](registrationId(user), agent) match {
      case Some(x) => {
        val agentId = agentMicroService.create(toAgent(x)).get.uar.getOrElse("")

        keyStoreMicroService.deleteKeyStore(registrationId(user), agent)
        Ok(views.html.agents.registration.thank_you_page(agentId))
      }
      case _ => Redirect(routes.AgentContactDetailsController.contactDetails())
    }
  }

  def step: String = thankYouName
}
