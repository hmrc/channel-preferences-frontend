package controllers.agent.registration

import controllers.common.{ ActionWrappers, SessionTimeoutWrapper, BaseController }
import uk.gov.hmrc.microservice.paye.domain.PayeRegime

class AgentTypeAndLegalEntityController extends BaseController with SessionTimeoutWrapper with ActionWrappers {

  def agentType = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(Some(PayeRegime)) {
      user =>
        implicit request =>
          Ok("Agent type and legal entity")
    }
  }
}
