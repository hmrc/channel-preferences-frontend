package controllers.common.service

import uk.gov.hmrc.microservice.txqueue.TxQueueMicroService
import uk.gov.hmrc.microservice.auth.AuthMicroService
import uk.gov.hmrc.microservice.paye.PayeMicroService
import uk.gov.hmrc.microservice.saml.SamlMicroService
import uk.gov.hmrc.microservice.sa.SaMicroService
import uk.gov.hmrc.microservice.governmentgateway.GovernmentGatewayMicroService
import uk.gov.hmrc.common.microservice.audit.AuditMicroService
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import uk.gov.hmrc.common.microservice.agent.AgentMicroService

trait MicroServices {

  implicit val authMicroService = new AuthMicroService()
  implicit val payeMicroService = new PayeMicroService()
  implicit val samlMicroService = new SamlMicroService()
  implicit val saMicroService = new SaMicroService()
  implicit val governmentGatewayMicroService = new GovernmentGatewayMicroService()
  implicit val txQueueMicroService = new TxQueueMicroService()
  implicit val auditMicroService = new AuditMicroService()
  implicit val keyStoreMicroService = new KeyStoreMicroService()
  implicit val agentMicroService = new AgentMicroService()

}
