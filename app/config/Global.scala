package config

import com.kenshoo.play.metrics.{Metrics, MetricsFilterImpl}
import connectors.WsHttp
import controllers.auth.AuthenticatedRequest
import controllers.filters.ExceptionHandlingFilter
import controllers.internal.OptInCohortConfigurationValues
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{EssentialFilter, Request, RequestHeader, Result}
import play.api.{Application, Configuration, Play}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http.HttpGet
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig
import uk.gov.hmrc.play.frontend.filters.{FrontendAuditFilter, FrontendLoggingFilter}
import play.api.mvc.{Request, RequestHeader, Result, Results}

import uk.gov.hmrc.auth.core._

object Global extends DefaultFrontendGlobal with RunMode with ServicesConfig {

  override def frontendFilters: Seq[EssentialFilter] = super.frontendFilters :+ ExceptionHandlingFilter

  override val auditConnector = Audit

  override def onStart(app: Application) {
    super.onStart(app)
    ApplicationCrypto.verifyConfiguration()
    OptInCohortConfigurationValues.verifyConfiguration()
  }

  def microserviceMetricsConfig(implicit app: Application): Option[Configuration] =
    app.configuration.getConfig(s"$env.microservice.metrics")

  override def loggingFilter: FrontendLoggingFilter = FrontendFilters.LoggingFilter

  override def frontendAuditFilter: FrontendAuditFilter = FrontendFilters.AuditFilter

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html = {
    implicit val authRequest: AuthenticatedRequest[_] = AuthenticatedRequest(request, None, None, None, None)
    views.html.error_template(pageTitle, heading, message)
  }

  override def metricsFilter = new MetricsFilterImpl(Play.current.injector.instanceOf[Metrics]) {

    override val knownStatuses = Seq(
      200, 201, 202, 204, 206,
      301, 302, 303, 304, 307, 308,
      400, 401, 403, 404, 408, 409, 410, 412, 413, 414, 417, 422, 499,
      500, 502, 503, 504
    )
  }

}

object Audit extends AuditConnector with AppName with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"$env.auditing")
}


