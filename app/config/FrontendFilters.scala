package config

import play.api.Mode.Mode
import play.api.{Configuration, Play}
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.frontend.filters.{FrontendAuditFilter, FrontendLoggingFilter, MicroserviceFilterSupport}

object FrontendFilters extends RunMode {

  import play.api.Play.current

  val LoggingFilter = new FrontendLoggingFilter with MicroserviceFilterSupport {
    override def controllerNeedsLogging(controllerName: String) = Play.configuration.getBoolean(s"controllers.$controllerName.needsLogging").getOrElse(true)
  }

  val AuditFilter = new FrontendAuditFilter with MicroserviceFilterSupport with AppName {
    val maskedFormFields = Seq()
    val applicationPort = None
    val auditConnector = Audit

    def controllerNeedsAuditing(controllerName: String) =
      Play.configuration.getBoolean(s"controllers.$controllerName.needsAuditing").getOrElse(true)

    override protected def appNameConfiguration: Configuration = Play.current.configuration
  }

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
