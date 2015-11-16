package connectors

import java.net.URLEncoder

import config.Audit
import play.api.libs.json._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws.WSGet
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}

import scala.concurrent.Future

trait EmailConnector extends HttpGet with ServicesConfig with AppName {
  protected def serviceUrl: String

  def isValid(emailAddress: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    implicit val readValidBoolean = (__ \ "valid").read[Boolean]
    GET[Boolean](s"$serviceUrl/validate-email-address?email=${URLEncoder.encode(emailAddress, "UTF-8")}")
  }
}
object EmailConnector extends EmailConnector with HttpAuditing with WSGet{
  val serviceUrl = baseUrl("email")

  override val hooks: Seq[HttpHook] = Seq(AuditingHook)
  override val auditConnector = Audit

}
