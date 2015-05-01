package controllers.sa.prefs.config

import connectors.HttpVerbs
import controllers.sa.prefs.internal.OptInCohortConfigurationValues
import play.api.mvc.Request
import play.api.{Application, Configuration}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.messagerenderer.config.FrontendFilters
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.config.{ServicesConfig, RunMode}
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.http.HttpGet
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter

object Global extends DefaultFrontendGlobal with RunMode with ServicesConfig {

  lazy val auditConnector: AuditConnector = AuditConnector(LoadAuditingConfig(s"$env.auditing"))

  lazy val authConnector: AuthConnector = new AuthConnector {
    lazy val http: HttpGet = HttpVerbs

    val serviceUrl: String = baseUrl("auth")
  }

  override def onStart(app: Application) {
    super.onStart(app)
    ApplicationCrypto.verifyConfiguration()
    OptInCohortConfigurationValues.verifyConfiguration()
  }

  def microserviceMetricsConfig(implicit app: Application): Option[Configuration] =
    app.configuration.getConfig(s"$env.microservice.metrics")

  override def loggingFilter: FrontendLoggingFilter = FrontendFilters.LoggingFilter

  override def frontendAuditFilter: FrontendAuditFilter = FrontendFilters.AuditFilter

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html = Html("")
}
