package uk.gov.hmrc.common.microservice

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import controllers.common.service.MicroServices
import uk.gov.hmrc.common.microservice.saml.SamlMicroService
import uk.gov.hmrc.microservice.txqueue.TxQueueMicroService
import uk.gov.hmrc.common.microservice.audit.AuditMicroService
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.governmentgateway.GovernmentGatewayMicroService
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import org.mockito.Mockito
import uk.gov.hmrc.common.microservice.agent.AgentMicroServiceRoot
import uk.gov.hmrc.common.microservice.vat.VatConnector

@deprecated("Uses Microservices, mock your injected services directly", "29.10.13")
trait MockMicroServicesForTests extends MicroServices with MockitoSugar {

  override lazy val authMicroService = mock[AuthMicroService]
  override lazy val payeMicroService = mock[PayeMicroService]
  override lazy val samlMicroService = mock[SamlMicroService]
  override lazy val saConnector = mock[SaConnector]
  override lazy val governmentGatewayMicroService = mock[GovernmentGatewayMicroService]
  override lazy val txQueueMicroService = mock[TxQueueMicroService]
  override lazy val auditMicroService = mock[AuditMicroService]
  override lazy val keyStoreMicroService = mock[KeyStoreMicroService]
  override lazy val agentMicroServiceRoot = mock[AgentMicroServiceRoot]
  override lazy val vatConnector = mock[VatConnector]

  private val mocks = List(
    authMicroService,
    payeMicroService,
    samlMicroService,
    saConnector,
    governmentGatewayMicroService,
    txQueueMicroService,
    auditMicroService,
    keyStoreMicroService,
    agentMicroServiceRoot)

  def resetAll() {
    Mockito.reset(mocks: _*)
  }
}
