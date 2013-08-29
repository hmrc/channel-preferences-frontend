package controllers.agent.registration

import controllers.common.{ ActionWrappers, SessionTimeoutWrapper, BaseController }
import uk.gov.hmrc.microservice.paye.domain.PayeRegime
import uk.gov.hmrc.microservice.domain.User
import play.api.mvc.{ Result, Request }

class AgentThankYouController extends BaseController with SessionTimeoutWrapper with ActionWrappers with AgentController {

  def thankYou = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { user => request => thankYouAction(user, request) } }

  private[registration] val thankYouAction: ((User, Request[_]) => Result) = (user, request) => {
    val uarFound = keyStoreMicroService.getKeyStore(uar(user), agent) match {
      case Some(x) => x.get(uar).getOrElse(Map.empty[String, String]).get(uar).getOrElse("Unknown")
      case _ => "Unknown"
    }
    keyStoreMicroService.deleteKeyStore(uar(user), agent)
    Ok(views.html.agents.registration.thank_you_page(uarFound))
  }
}
