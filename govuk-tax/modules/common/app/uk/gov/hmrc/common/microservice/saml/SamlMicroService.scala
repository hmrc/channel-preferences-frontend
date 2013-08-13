package uk.gov.hmrc.microservice.saml

import play.api.libs.json.Json
import uk.gov.hmrc.microservice.{ MicroServiceConfig, MicroService }
import uk.gov.hmrc.microservice.saml.domain.{ AuthResponseValidationResult, AuthRequestFormData }

class SamlMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.samlServiceUrl

  def create = httpGet[AuthRequestFormData]("/saml/create")
    .getOrElse(throw new IllegalStateException("Expected SAML auth response but none returned"))

  def validate(authResponse: String) = httpPost[AuthResponseValidationResult](
    "/saml/validate", Json.toJson(Map("samlResponse" -> authResponse)), Map.empty)
    .getOrElse(throw new IllegalStateException("Expected SAML validation response but none returned"))
}
