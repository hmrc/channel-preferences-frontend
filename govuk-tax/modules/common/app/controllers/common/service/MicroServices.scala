package controllers.common.service


@deprecated("this will eventually be removed. All new controllers should wire in the services that they require", "")
trait MicroServices {

  import uk.gov.hmrc.common.microservice.auth.AuthMicroService
  import uk.gov.hmrc.common.microservice.paye.PayeMicroService
  import uk.gov.hmrc.common.microservice.audit.AuditMicroService
  import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
  import uk.gov.hmrc.common.microservice.agent.AgentMicroService
  import uk.gov.hmrc.common.microservice.vat.VatConnector
  import uk.gov.hmrc.common.microservice.saml.SamlMicroService
  import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
  import uk.gov.hmrc.common.microservice.ct.CtConnector
  import uk.gov.hmrc.microservice.txqueue.TxQueueMicroService
  import uk.gov.hmrc.common.microservice.sa.SaConnector
  import uk.gov.hmrc.microservice.governmentgateway.GovernmentGatewayMicroService

  implicit lazy val authMicroService = new AuthMicroService()
  implicit lazy val payeMicroService = new PayeMicroService()
  implicit lazy val samlMicroService = new SamlMicroService()
  implicit lazy val saConnector = new SaConnector()
  implicit lazy val governmentGatewayMicroService = new GovernmentGatewayMicroService()
  implicit lazy val txQueueMicroService = new TxQueueMicroService()
  implicit lazy val auditMicroService = new AuditMicroService()
  implicit lazy val keyStoreMicroService = new KeyStoreMicroService()
  implicit lazy val agentMicroService = new AgentMicroService()
  implicit lazy val vatConnector = new VatConnector()
  implicit lazy val ctConnector = new CtConnector()
  implicit lazy val epayeConnector = new EpayeConnector()

}

object MicroServices extends MicroServices