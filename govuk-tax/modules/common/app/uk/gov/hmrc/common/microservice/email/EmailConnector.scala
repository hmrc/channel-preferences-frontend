package uk.gov.hmrc.common.microservice.email

import domain.ValidateEmailResponse
import uk.gov.hmrc.microservice.{MicroServiceConfig, Connector}
import java.net.URLEncoder
import controllers.common.actions.HeaderCarrier

class EmailConnector extends Connector {

  override val serviceUrl = MicroServiceConfig.emailServiceUrl

  def validateEmailAddress(address: String)(implicit hc: HeaderCarrier): Boolean = {
    val v = httpGet[ValidateEmailResponse](s"/validate-email-address?email=${URLEncoder.encode(address, "UTF-8")}")
    v.map(_.valid).getOrElse(throw new RuntimeException(s"Access to resource: '/validate-email-address' gave an invalid response"))
  }

}
