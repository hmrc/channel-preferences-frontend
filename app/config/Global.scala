package config

import com.kenshoo.play.metrics.{MetricsFilter, MetricsRegistry}
import connectors.WsHttp
import controllers.sa.prefs.internal.OptInCohortConfigurationValues
import play.api.mvc.Request
import play.api.{Application, Configuration}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.http.HttpGet
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter

object Global extends DefaultFrontendGlobal with RunMode with ServicesConfig {

  lazy val auditConnector: AuditConnector = AuditConnector(LoadAuditingConfig(s"$env.auditing"))

  lazy val authConnector: AuthConnector = new AuthConnector {
    lazy val http: HttpGet = WsHttp

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

  override def metricsFilter = new MetricsFilter {
    def registry = MetricsRegistry.defaultRegistry
    override val knownStatuses = Seq(
      200, 201, 202, 204, 206,
      301, 302, 303, 304, 307, 308,
      400, 401, 403, 404, 408, 409, 410, 412, 413, 414, 417, 422, 499,
      500, 502, 503, 504
    )
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html = Html("")
}
