package uk.gov.hmrc.microservice

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.microservice.auth.AuthMicroService
import uk.gov.hmrc.microservice.paye.PayeMicroService
import controllers.common.service.MicroServices
import uk.gov.hmrc.microservice.saml.SamlMicroService
import uk.gov.hmrc.microservice.txqueue.TxQueueMicroService
import uk.gov.hmrc.common.microservice.audit.AuditMicroService

trait MockMicroServicesForTests extends MicroServices with MockitoSugar {

  override val authMicroService = mock[AuthMicroService]
  override val payeMicroService = mock[PayeMicroService]
  override val samlMicroService = mock[SamlMicroService]
  override val txQueueMicroService = mock[TxQueueMicroService]
  override val auditMicroService = mock[AuditMicroService]
}
