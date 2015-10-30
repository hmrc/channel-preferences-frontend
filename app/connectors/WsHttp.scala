package connectors

import controllers.sa.prefs.config.Global
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost, WSPut}

object WsHttp extends WSGet with WSPut with WSPost with AppName with RunMode {
  override def auditConnector: AuditConnector = Global.auditConnector
}
