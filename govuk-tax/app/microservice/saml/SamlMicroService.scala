package microservice.saml

import microservice.{ MicroServiceConfig, MicroService }
import scala.concurrent.Await
import microservice.saml.domain.{ AuthResponseValidationResult, AuthRequestFormData }
import play.api.libs.json.Json

class SamlMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.samlServiceUrl

  def create = httpGet[AuthRequestFormData]("/saml/create")
    .getOrElse(throw new IllegalStateException("Expected SAML auth response but none returned"))

  def validate(authResponse: String) = httpPost[AuthResponseValidationResult](
    "/saml/validate", Json.toJson(Map("authResponse" -> authResponse)))
    .getOrElse(throw new IllegalStateException("Expected SAML validation response but none returned"))
}
