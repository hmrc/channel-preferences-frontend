package controllers.common.service

import uk.gov.hmrc.microservice.txqueue.TxQueueMicroService
import uk.gov.hmrc.microservice.auth.AuthMicroService
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import uk.gov.hmrc.microservice.saml.SamlMicroService
import uk.gov.hmrc.microservice.sa.SaMicroService
import uk.gov.hmrc.microservice.governmentgateway.GovernmentGatewayMicroService
import uk.gov.hmrc.common.microservice.audit.AuditMicroService
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import uk.gov.hmrc.common.microservice.agent.AgentMicroService
import uk.gov.hmrc.common.microservice.vat.VatMicroService

@deprecated("this will eventually be removed. All new controllers should wire in the services that they require")
trait MicroServices {

  implicit lazy val authMicroService = new AuthMicroService()
  implicit lazy val payeMicroService = new PayeMicroService()
  implicit lazy val samlMicroService = new SamlMicroService()
  implicit lazy val saMicroService = new SaMicroService()
  implicit lazy val governmentGatewayMicroService = new GovernmentGatewayMicroService()
  implicit lazy val txQueueMicroService = new TxQueueMicroService()
  implicit lazy val auditMicroService = new AuditMicroService()
  implicit lazy val keyStoreMicroService = new KeyStoreMicroService()
  implicit lazy val agentMicroService = new AgentMicroService()
  implicit lazy val vatMicroService = new VatMicroService()

}
