package uk.gov.hmrc.microservice

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.microservice.auth.AuthMicroService
import uk.gov.hmrc.microservice.paye.PayeMicroService
import controllers.service.MicroServices
import uk.gov.hmrc.microservice.saml.SamlMicroService

trait MockMicroServicesForTests extends MicroServices with MockitoSugar {

  override val authMicroService = mock[AuthMicroService]
  override val payeMicroService = mock[PayeMicroService]
  override val samlMicroService = mock[SamlMicroService]
}
