package microservice.saml

import microservice.{MicroServiceConfig, MicroService}
import scala.concurrent.Await
import microservice.saml.domain.{AuthResponseValidationData, AuthResponseValidationResult, AuthRequestFormData}

class SamlMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.payeServiceUrl

  def create = Await.result(response[AuthRequestFormData](httpResource("/saml/create").get()), defaultTimeoutDuration)

  def validate(authResponse: String) = Await.result(response[AuthResponseValidationResult](
    httpResource("/saml/validate").post(AuthResponseValidationData(authResponse))), defaultTimeoutDuration)
}
