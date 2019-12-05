package connectors

import akka.actor.ActorSystem
import com.typesafe.config.Config
import config.Audit
import play.api.Mode.Mode
import play.api.{Configuration, Play}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost, WSPut}


object WsHttp extends HttpGet with WSGet with HttpPut with WSPut with HttpPost with WSPost with AppName with HttpAuditing with RunMode {
  override val hooks = Seq(AuditingHook)
  override val auditConnector: AuditConnector = Audit

  override protected def appNameConfiguration: Configuration = Play.current.configuration

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration

  override protected def actorSystem: ActorSystem = Play.current.actorSystem

  override protected def configuration: Option[Config] = Some(Play.current.configuration.underlying)
}
