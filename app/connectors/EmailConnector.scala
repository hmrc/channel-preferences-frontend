package connectors

import java.net.URLEncoder

import config.{ServicesCircuitBreaker, Audit}
import play.api.Logger
import play.api.libs.json._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws.WSGet
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait EmailConnector extends HttpGet with AppName with ServicesCircuitBreaker { this: ServicesConfig =>
  protected def serviceUrl: String

  override val externalServiceName = "email"

  def isValid(emailAddress: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    implicit val readValidBoolean = (__ \ "valid").read[Boolean]
    withCircuitBreaker(GET[Boolean](s"$serviceUrl/validate-email-address?email=${URLEncoder.encode(emailAddress, "UTF-8")}") recover {
      case e => {
        Logger.error(s"Could not contact EMAIL service and validate email address for ${emailAddress}: ${e.getMessage}")
        false
      }
    })
  }
}

object EmailConnector extends EmailConnector with HttpAuditing with ServicesConfig with WSGet {
  val serviceUrl = baseUrl("email")

  override val hooks: Seq[HttpHook] = Seq(AuditingHook)
  override val auditConnector = Audit

}
