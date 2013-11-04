package controllers.paye

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.saml.SamlConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.governmentgateway.GovernmentGatewayConnector
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import org.mockito.Mockito
import uk.gov.hmrc.common.microservice.agent.AgentConnectorRoot
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector

@deprecated("Delete this trait once the BenefitHomeControllerSpec and PayeHomeControllerSpec are deleted", "30.10.13")
private [paye] trait MockConnectorsForTests extends Connectors with MockitoSugar {

  override lazy val authConnector = mock[AuthConnector]
  override lazy val payeConnector = mock[PayeConnector]
  override lazy val samlConnector = mock[SamlConnector]
  override lazy val saConnector = mock[SaConnector]
  override lazy val governmentGatewayConnector = mock[GovernmentGatewayConnector]
  override lazy val txQueueConnector = mock[TxQueueConnector]
  override lazy val auditConnector = mock[AuditConnector]
  override lazy val keyStoreConnector = mock[KeyStoreConnector]
  override lazy val agentConnectorRoot = mock[AgentConnectorRoot]
  override lazy val vatConnector = mock[VatConnector]

  private val mocks = List(
    authConnector,
    payeConnector,
    samlConnector,
    saConnector,
    governmentGatewayConnector,
    txQueueConnector,
    auditConnector,
    keyStoreConnector,
    agentConnectorRoot)

  def resetAll() {
    Mockito.reset(mocks: _*)
  }
}
