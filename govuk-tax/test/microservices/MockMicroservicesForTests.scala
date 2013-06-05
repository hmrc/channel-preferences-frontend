package microservices

import org.scalatest.mock.MockitoSugar
import microservice.auth.AuthMicroService
import microservice.paye.PayeMicroService
import controllers.service.MicroServices

trait MockMicroServicesForTests extends MicroServices with MockitoSugar {

  override val authMicroService = mock[AuthMicroService]
  override val payeMicroService = mock[PayeMicroService]

}
