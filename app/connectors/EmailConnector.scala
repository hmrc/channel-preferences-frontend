package connectors

import java.net.URLEncoder

import play.api.libs.json._
import uk.gov.hmrc.common.microservice.MicroServiceConfig
import uk.gov.hmrc.play.connectors.HeaderCarrier
import uk.gov.hmrc.play.http.HttpGet
import uk.gov.hmrc.play.http.ws.WSGet

import scala.concurrent.Future

trait EmailConnector extends HttpGet {
  protected def serviceUrl: String

  def isValid(emailAddress: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    implicit val readValidBoolean = (__ \ "valid").read[Boolean]
    GET[Boolean](s"$serviceUrl/validate-email-address?email=${URLEncoder.encode(emailAddress, "UTF-8")}")
  }
}
object EmailConnector extends EmailConnector with WSGet {
  val serviceUrl = MicroServiceConfig.emailServiceUrl
}
