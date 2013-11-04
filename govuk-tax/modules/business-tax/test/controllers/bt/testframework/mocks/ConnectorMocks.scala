package controllers.bt.testframework.mocks

import org.scalatest.mock.MockitoSugar
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.saml.SamlConnector
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.governmentgateway.GovernmentGatewayConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.agent.AgentConnectorRoot
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector

trait ConnectorMocks extends MockitoSugar with Connectors {

  val mockAuthConnector = mock[AuthConnector]
  val mockPayeConnector = mock[PayeConnector]
  val mockSamlConnector = mock[SamlConnector]
  val mockSaConnector = mock[SaConnector]
  val mockGovernmentGatewayConnector = mock[GovernmentGatewayConnector]
  val mockTxQueueConnector = mock[TxQueueConnector]
  val mockAuditConnector = mock[AuditConnector]
  val mockKeyStoreConnector = mock[KeyStoreConnector]
  val mockAgentConnectorRoot = mock[AgentConnectorRoot]
  val mockVatConnector = mock[VatConnector]
  val mockCtConnector = mock[CtConnector]
  val mockEpayeConnector = mock[EpayeConnector]

  trait MockedConnectors {

    self: Connectors =>

    override lazy val authConnector = mockAuthConnector
    override lazy val payeConnector = mockPayeConnector
    override lazy val samlConnector = mockSamlConnector
    override lazy val saConnector = mockSaConnector
    override lazy val governmentGatewayConnector = mockGovernmentGatewayConnector
    override lazy val txQueueConnector = mockTxQueueConnector
    override lazy val auditConnector = mockAuditConnector
    override lazy val keyStoreConnector = mockKeyStoreConnector
    override lazy val agentConnectorRoot = mockAgentConnectorRoot
    override lazy val vatConnector = mockVatConnector
    override lazy val ctConnector = mockCtConnector
    override lazy val epayeConnector = mockEpayeConnector
  }

}
