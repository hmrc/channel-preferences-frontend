package controllers.agent.registration

import controllers.common.{Ida, Actions, BaseController2}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{SimpleResult, Request}
import controllers.agent.registration.FormNames._
import controllers.common.actions.MultiFormWrapper
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import service.agent.AgentConnector

class AgentThankYouController(override val auditConnector: AuditConnector,
                              override val keyStoreConnector: KeyStoreConnector)
                             (implicit agentMicroService : AgentConnector,
                              override val authConnector: AuthConnector)
  extends BaseController2
  with Actions
  with AgentController
  with MultiFormWrapper
  with AgentMapper {

  def this() = this(Connectors.auditConnector, Connectors.keyStoreConnector)(AgentConnector(), Connectors.authConnector)

  def thankYou = ActionAuthorisedBy(Ida)(Some(PayeRegime)) {
    MultiFormAction(multiFormConfig) {
      user => request => thankYouAction(user, request)
    }
  }

  private[registration] val thankYouAction: ((User, Request[_]) => SimpleResult) = (user, request) => {
    keyStoreConnector.getKeyStore[Map[String, String]](registrationId(user), agent) match {
      case Some(x) => {
        val agentId = agentMicroService.create(user.regimes.paye.get.nino, toAgent(x)).uar

        keyStoreConnector.deleteKeyStore(registrationId(user), agent)
        Ok(views.html.agents.registration.thank_you_page(agentId))
      }
      case _ => Redirect(routes.AgentContactDetailsController.contactDetails())
    }
  }

  def step: String = thankYouName
}
