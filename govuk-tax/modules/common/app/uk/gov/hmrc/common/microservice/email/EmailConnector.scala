package uk.gov.hmrc.common.microservice.email

import domain.ValidateEmailResponse
import uk.gov.hmrc.microservice.{MicroServiceConfig, Connector}
import java.net.URLEncoder
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class EmailConnector extends Connector {

  override val serviceUrl = MicroServiceConfig.emailServiceUrl

  def validateEmailAddress(address: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    httpGetF[ValidateEmailResponse](s"/validate-email-address?email=${URLEncoder.encode(address, "UTF-8")}").map {
      case Some(ValidateEmailResponse(valid)) => valid
      case _ => throw new RuntimeException(s"Access to resource: '/validate-email-address' gave an invalid response")
    }
  }

}
