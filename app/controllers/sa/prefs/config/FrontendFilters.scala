package uk.gov.hmrc.messagerenderer.config

import play.api.Play
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter

object FrontendFilters extends RunMode {

  import play.api.Play.current

  val LoggingFilter = new FrontendLoggingFilter {
    override def controllerNeedsLogging(controllerName: String) = Play.configuration.getBoolean(s"controllers.$controllerName.needsLogging").getOrElse(true)
  }

  val AuditFilter = new FrontendAuditFilter with AppName {
    val maskedFormFields = Seq()
    val applicationPort = None
    val auditConnector = AuditConnector(LoadAuditingConfig(s"$env.auditing"))

    def controllerNeedsAuditing(controllerName: String) =
      Play.configuration.getBoolean(s"$env.controllers.$controllerName.needsAuditing").getOrElse(true)
  }
}