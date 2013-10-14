package controllers.bt

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import uk.gov.hmrc.common.microservice.saml.SamlMicroService
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.microservice.governmentgateway.GovernmentGatewayMicroService
import uk.gov.hmrc.microservice.txqueue.TxQueueMicroService
import uk.gov.hmrc.common.microservice.audit.AuditMicroService
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import uk.gov.hmrc.common.microservice.agent.AgentMicroService
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import controllers.common.service.MicroServices

private[bt] trait MicroServiceMocks extends MockitoSugar {
  val mockAuthMicroService = mock[AuthMicroService]
  val mockPayeMicroService = mock[PayeMicroService]
  val mockSamlMicroService = mock[SamlMicroService]
  val mockSaConnector = mock[SaConnector]
  val mockGovernmentGatewayMicroService = mock[GovernmentGatewayMicroService]
  val mockTxQueueMicroService = mock[TxQueueMicroService]
  val mockAuditMicroService = mock[AuditMicroService]
  val mockKeyStoreMicroService = mock[KeyStoreMicroService]
  val mockAgentMicroService = mock[AgentMicroService]
  val mockVatConnector = mock[VatConnector]
  val mockCtConnector = mock[CtConnector]
  val mockEpayeConnector = mock[EpayeConnector]

  trait MockedMicroServices extends MicroServices {
    override lazy val authMicroService = mockAuthMicroService
    override lazy val payeMicroService = mockPayeMicroService
    override lazy val samlMicroService = mockSamlMicroService
    override lazy val saConnector = mockSaConnector
    override lazy val governmentGatewayMicroService = mockGovernmentGatewayMicroService
    override lazy val txQueueMicroService = mockTxQueueMicroService
    override lazy val auditMicroService = mockAuditMicroService
    override lazy val keyStoreMicroService = mockKeyStoreMicroService
    override lazy val agentMicroService = mockAgentMicroService
    override lazy val vatConnector = mockVatConnector
    override lazy val ctConnector = mockCtConnector
    override lazy val epayeConnector = mockEpayeConnector
  }

}