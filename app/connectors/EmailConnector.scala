package connectors

import akka.actor.ActorSystem
import com.typesafe.config.Config
import config.{Audit, ServicesCircuitBreaker}
import play.api.Mode.Mode
import play.api.libs.json._
import play.api.{Configuration, Logger, Play}
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.ws.WSPost

import scala.concurrent.Future

trait EmailConnector extends HttpPost with AppName with ServicesCircuitBreaker { this: ServicesConfig =>
  protected def serviceUrl: String

  override val externalServiceName = "email"

  def isValid(emailAddress: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    implicit val readValidBoolean = (__ \ "valid").read[Boolean]
    withCircuitBreaker(POST[UpdateEmail,Boolean](s"$serviceUrl/validate-email-address", UpdateEmail(emailAddress)) recover {
      case e => {
        Logger.error(s"Could not contact EMAIL service and validate email address for ${emailAddress}: ${e.getMessage}")
        false
      }
    })
  }
}

object EmailConnector extends EmailConnector with HttpAuditing with ServicesConfig with WSPost {
  val serviceUrl = baseUrl("email")

  override val hooks: Seq[HttpHook] = Seq(AuditingHook)
  override val auditConnector = Audit

  override protected def actorSystem: ActorSystem = Play.current.actorSystem

  override protected def configuration: Option[Config] = Some(Play.current.configuration.underlying)

  override protected def appNameConfiguration: Configuration = Play.current.configuration

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
