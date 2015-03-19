package connectors

import java.net.URLEncoder

import play.api.libs.json._
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, AuditConnector, ServicesConfig}
import uk.gov.hmrc.play.http.HttpGet
import uk.gov.hmrc.play.http.ws.WSGet

import scala.concurrent.Future

trait EmailConnector extends HttpGet with ServicesConfig with AppName {
  protected def serviceUrl: String

  def isValid(emailAddress: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    implicit val readValidBoolean = (__ \ "valid").read[Boolean]
    GET[Boolean](s"$serviceUrl/validate-email-address?email=${URLEncoder.encode(emailAddress, "UTF-8")}")
  }
}
object EmailConnector extends EmailConnector with WSGet {
  val serviceUrl = baseUrl("email")

  override def auditConnector: AuditConnector = AuditConnector
}
