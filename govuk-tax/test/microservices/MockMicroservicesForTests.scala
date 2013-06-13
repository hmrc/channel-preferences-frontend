package microservices

import org.scalatest.mock.MockitoSugar
import microservice.auth.AuthMicroService
import microservice.paye.SaMicroService
import controllers.service.MicroServices
import microservice.saml.SamlMicroService

trait MockMicroServicesForTests extends MicroServices with MockitoSugar {

  override val authMicroService = mock[AuthMicroService]
  override val payeMicroService = mock[SaMicroService]
  override val samlMicroService = mock[SamlMicroService]
}
