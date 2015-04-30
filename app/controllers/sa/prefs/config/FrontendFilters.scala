package controllers.sa.prefs.config

import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter

object FrontendFilters {

  val LoggingFilter = new FrontendLoggingFilter {
    override def controllerNeedsLogging(controllerName: String) = true
  }

  val AuditFilter = new FrontendAuditFilter with AppName {

    override def maskedFormFields: Seq[String] = Seq()

    override def applicationPort: Option[Int] = None

    override def auditConnector: AuditConnector = Global.auditConnector

    override def controllerNeedsAuditing(controllerName: String) = true
  }
}
