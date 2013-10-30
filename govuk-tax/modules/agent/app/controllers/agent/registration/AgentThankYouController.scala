package controllers.agent.registration

import controllers.common.{Ida, Actions, BaseController2}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{SimpleResult, Request}
import controllers.agent.registration.FormNames._
import controllers.common.actions.MultiFormWrapper
import uk.gov.hmrc.common.microservice.audit.AuditMicroService
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import controllers.common.service.MicroServices
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import service.agent.AgentMicroService

class AgentThankYouController(override val auditMicroService: AuditMicroService,
                              override val keyStoreMicroService: KeyStoreMicroService)
                             (implicit agentMicroService : AgentMicroService,
                              override val authMicroService: AuthMicroService)
  extends BaseController2
  with Actions
  with AgentController
  with MultiFormWrapper
  with AgentMapper {

  def this() = this(MicroServices.auditMicroService, MicroServices.keyStoreMicroService)(AgentMicroService(), MicroServices.authMicroService)

  def thankYou = ActionAuthorisedBy(Ida)(Some(PayeRegime)) {
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
