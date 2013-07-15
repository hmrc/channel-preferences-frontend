package microservice

import org.scalatest.mock.MockitoSugar
import microservice.auth.AuthMicroService
import microservice.paye.PayeMicroService
import controllers.service.MicroServices
import microservice.saml.SamlMicroService

trait MockMicroServicesForTests extends MicroServices with MockitoSugar {

  override val authMicroService = mock[AuthMicroService]
  override val payeMicroService = mock[PayeMicroService]
  override val samlMicroService = mock[SamlMicroService]
}
