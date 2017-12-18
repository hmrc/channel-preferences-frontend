package connectors

import config.{Audit, ServicesCircuitBreaker}
import play.api.Logger
import play.api.libs.json._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.ws.WSPost
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.http.hooks.HttpHook

trait EmailConnector extends HttpPost with AppName with ServicesCircuitBreaker { this: ServicesConfig =>
  protected def serviceUrl: String

  override val externalServiceName = "email"

  def isValid(emailAddress: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    implicit val readValidBoolean = (__ \ "valid").read[Boolean]
    withCircuitBreaker(POST[UpdateEmail, Boolean](s"$serviceUrl/validate-email-address", UpdateEmail(emailAddress)) recover {
      case e =>
        Logger.error(s"Could not contact EMAIL service and validate email address: ${e.getMessage}")
        false
    })
  }




}

object EmailConnector extends EmailConnector with HttpAuditing with ServicesConfig with WSPost {
  val serviceUrl = baseUrl("email")

  override val hooks: Seq[HttpHook] = Seq(AuditingHook)
  override val auditConnector = Audit

}
