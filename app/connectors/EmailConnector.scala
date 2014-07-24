package connectors

import java.net.URLEncoder

import uk.gov.hmrc.common.microservice.{Connector, MicroServiceConfig}
import uk.gov.hmrc.play.connectors.HeaderCarrier

import scala.concurrent.Future

class EmailConnector extends Connector {

  override protected val serviceUrl = MicroServiceConfig.emailServiceUrl

  def validateEmailAddress(address: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    httpGetF[ValidateEmailResponse](s"/validate-email-address?email=${URLEncoder.encode(address, "UTF-8")}").map {
      case Some(ValidateEmailResponse(valid)) => valid
      case _ => throw new RuntimeException(s"Access to resource: '/validate-email-address' gave an invalid response")
    }

}
object EmailConnector extends EmailConnector
case class ValidateEmailResponse(valid: Boolean)