package uk.gov.hmrc.common.microservice.saml

import uk.gov.hmrc.common.microservice.{MicroServiceConfig, Connector}
import uk.gov.hmrc.microservice.saml.domain.{AuthResponseValidationResult, AuthRequestFormData}
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future


class SamlConnector extends Connector {
  override val serviceUrl = MicroServiceConfig.samlServiceUrl

  def create(implicit hc: HeaderCarrier) = httpGetF[AuthRequestFormData]("/saml/create")
    .map(_.getOrElse(throw new IllegalStateException("Expected SAML auth response but none returned")))

  def validate(authResponse: String)(implicit hc: HeaderCarrier): Future[AuthResponseValidationResult] = {
    httpPostF[AuthResponseValidationResult, Map[String, String]](
      uri = "/saml/validate",
      body = Some(Map("samlResponse" -> authResponse))
    ).map(_.getOrElse(throw new IllegalStateException("Expected SAML validation response but none returned")))
  }
}


