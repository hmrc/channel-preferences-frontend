package uk.gov.hmrc.microservice

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.microservice.auth.AuthMicroService
import uk.gov.hmrc.microservice.paye.PayeMicroService
import controllers.common.service.MicroServices
import uk.gov.hmrc.microservice.saml.SamlMicroService
import uk.gov.hmrc.microservice.txqueue.TxQueueMicroService
import uk.gov.hmrc.common.microservice.audit.AuditMicroService
import uk.gov.hmrc.microservice.sa.SaMicroService
import uk.gov.hmrc.microservice.governmentgateway.GovernmentGatewayMicroService
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import org.mockito.Mockito

trait MockMicroServicesForTests extends MicroServices with MockitoSugar {

  override lazy val authMicroService = mock[AuthMicroService]
  override lazy val payeMicroService = mock[PayeMicroService]
  override lazy val samlMicroService = mock[SamlMicroService]
  override lazy val saMicroService = mock[SaMicroService]
  override lazy val governmentGatewayMicroService = mock[GovernmentGatewayMicroService]
  override lazy val txQueueMicroService = mock[TxQueueMicroService]
  override lazy val auditMicroService = mock[AuditMicroService]
  override lazy val keyStoreMicroService = mock[KeyStoreMicroService]

  private val mocks = List(authMicroService, payeMicroService, samlMicroService, saMicroService, governmentGatewayMicroService, txQueueMicroService, auditMicroService, keyStoreMicroService)

  def resetAll() {
    Mockito.reset(mocks: _*)
  }
}
