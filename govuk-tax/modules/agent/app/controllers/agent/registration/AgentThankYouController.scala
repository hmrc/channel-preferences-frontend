package controllers.agent.registration

import controllers.common.BaseController
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{SimpleResult, Request}
import controllers.agent.registration.FormNames._
import controllers.common.actions.{HeaderCarrier, Actions, MultiFormWrapper}
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import service.agent.AgentConnector
import controllers.agent.AgentsRegimeRoots
import scala.concurrent.Future

class AgentThankYouController(override val auditConnector: AuditConnector,
                              override val keyStoreConnector: KeyStoreConnector)
                             (implicit agentMicroService: AgentConnector,
                              override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with AgentController
  with MultiFormWrapper
  with AgentMapper
  with AgentsRegimeRoots {

  def this() = this(Connectors.auditConnector, Connectors.keyStoreConnector)(AgentConnector(), Connectors.authConnector)

  def thankYou = AuthorisedFor(PayeRegime).async {
    MultiFormAction.async(multiFormConfig) {
      user => request => thankYouAction(user, request)
    }
  }

  private[registration] val thankYouAction: ((User, Request[_]) => Future[SimpleResult]) = (user, request) => {
    implicit def hc = HeaderCarrier(request)
    keyStoreConnector.getKeyStore[Map[String, String]](actionId(), agent, true) flatMap {
      case Some(x) => {
        agentMicroService.create(user.regimes.paye.get.nino, toAgent(x)) map { uar =>

          keyStoreConnector.deleteKeyStore(actionId(), agent, true)
          Ok(views.html.agents.registration.thank_you_page(uar.uar))
        }
      }
      case _ => Future.successful(Redirect(routes.AgentContactDetailsController.contactDetails()))
    }
  }

  def step: String = thankYouName
}
