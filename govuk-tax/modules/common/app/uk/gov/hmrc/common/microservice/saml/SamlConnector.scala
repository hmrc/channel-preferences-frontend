package uk.gov.hmrc.common.microservice.saml

import play.api.libs.json.Json
import uk.gov.hmrc.microservice.{MicroServiceConfig, Connector}
import uk.gov.hmrc.microservice.saml.domain.{AuthResponseValidationResult, AuthRequestFormData}
import controllers.common.HeaderNames
import controllers.common.actions.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

class SamlConnector extends Connector with HeaderNames {

  override val serviceUrl = MicroServiceConfig.samlServiceUrl

  def create(implicit hc: HeaderCarrier) = httpGetF[AuthRequestFormData]("/saml/create")
    .map(_.getOrElse(throw new IllegalStateException("Expected SAML auth response but none returned")))

  def validate(authResponse: String)(implicit hc: HeaderCarrier) = {
    httpPostF[Map[String, String], AuthResponseValidationResult](
      "/saml/validate",
      Map("samlResponse" -> authResponse),
      Map.empty)
      .map(_.getOrElse(throw new IllegalStateException("Expected SAML validation response but none returned")))
  }
}
