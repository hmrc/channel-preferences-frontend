package controllers.bt.testframework.mocks

import org.scalatest.mock.MockitoSugar
import controllers.common.service.MicroServices
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import uk.gov.hmrc.common.microservice.saml.SamlMicroService
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.governmentgateway.GovernmentGatewayMicroService
import uk.gov.hmrc.microservice.txqueue.TxQueueMicroService
import uk.gov.hmrc.common.microservice.audit.AuditMicroService
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import uk.gov.hmrc.common.microservice.agent.AgentMicroServiceRoot
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector

trait ConnectorMocks extends MockitoSugar with MicroServices {

  val mockAuthMicroService = mock[AuthMicroService]
  val mockPayeMicroService = mock[PayeMicroService]
  val mockSamlMicroService = mock[SamlMicroService]
  val mockSaConnector = mock[SaConnector]
  val mockGovernmentGatewayMicroService = mock[GovernmentGatewayMicroService]
  val mockTxQueueMicroService = mock[TxQueueMicroService]
  val mockAuditMicroService = mock[AuditMicroService]
  val mockKeyStoreMicroService = mock[KeyStoreMicroService]
  val mockAgentMicroServiceRoot = mock[AgentMicroServiceRoot]
  val mockVatConnector = mock[VatConnector]
  val mockCtConnector = mock[CtConnector]
  val mockEpayeConnector = mock[EpayeConnector]

  trait MockedConnectors {

    self: MicroServices =>

    override lazy val authMicroService = mockAuthMicroService
    override lazy val payeMicroService = mockPayeMicroService
    override lazy val samlMicroService = mockSamlMicroService
    override lazy val saConnector = mockSaConnector
    override lazy val governmentGatewayMicroService = mockGovernmentGatewayMicroService
    override lazy val txQueueMicroService = mockTxQueueMicroService
    override lazy val auditMicroService = mockAuditMicroService
    override lazy val keyStoreMicroService = mockKeyStoreMicroService
    override lazy val agentMicroServiceRoot = mockAgentMicroServiceRoot
    override lazy val vatConnector = mockVatConnector
    override lazy val ctConnector = mockCtConnector
    override lazy val epayeConnector = mockEpayeConnector
  }

}
