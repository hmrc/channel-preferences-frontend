package config

import com.kenshoo.play.metrics.MetricsFilter
import com.codahale.metrics.graphite.{GraphiteReporter, Graphite}
import java.net.InetSocketAddress
import com.codahale.metrics.{MetricFilter, SharedMetricRegistries}
import java.util.concurrent.TimeUnit
import play.api._
import play.api.mvc._
import play.filters.csrf._
import scala.concurrent.Future
import play.api.mvc.Results._
import scala.Some
import play.api.i18n.Messages
import uk.gov.hmrc.common.filters.{CacheControlFilter, SessionCookieCryptoFilter, CSRFExceptionsFilter}
import uk.gov.hmrc.common.crypto.ApplicationCrypto
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import org.json4s.ext._Interval

object Global
  extends WithFilters(MetricsFilter,
                      SessionCookieCryptoFilter,
                      CSRFExceptionsFilter,
                      CSRFFilter(),
                      CacheControlFilter("image/", "text/css", "application/javascript")) {

  import controllers.common.service.Connectors._

  override def onStart(app: Application) {
    val env = app.mode
    if (app.configuration.getBoolean("metrics.enabled").getOrElse(false) &&
      app.configuration.getBoolean(s"govuk-tax.$env.metrics.graphite.enabled").getOrElse(false)) {
      startGraphite(app)
    }

    ApplicationCrypto.verifyConfiguration()
  }

  def startGraphite(app: Application) {
    val env = app.mode

    val graphite = new Graphite(new InetSocketAddress(
      app.configuration.getString(s"govuk-tax.$env.metrics.graphite.host").getOrElse("graphite"),
      app.configuration.getInt(s"govuk-tax.$env.metrics.graphite.port").getOrElse(2003)))

    val prefix = app.configuration.getString(s"govuk-tax.$env.metrics.graphite.prefix").getOrElse("tax")

    val reporter = GraphiteReporter.forRegistry(
      SharedMetricRegistries.getOrCreate(app.configuration.getString("metrics.name").getOrElse("default")))
      .prefixedWith(s"$prefix.${java.net.InetAddress.getLocalHost.getHostName}")
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .filter(MetricFilter.ALL)
      .build(graphite)

    reporter.start(app.configuration.getLong(s"govuk-tax.$env.metrics.graphite.interval").getOrElse(10L), TimeUnit.SECONDS)
  }

  // Play 2.0 doesn't support trailing slash: http://play.lighthouseapp.com/projects/82401/tickets/98
  override def onRouteRequest(request: RequestHeader) = super.onRouteRequest(request).orElse {
    Some(request.path).filter(_.endsWith("/")).flatMap(p => super.onRouteRequest(request.copy(path = p.dropRight(1))))
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    implicit val hc = HeaderCarrier(request)
    Future.successful {
      auditConnector.audit(createAuditEvent("ServerInternalError", "Unexpected error", request, hc, Option(ex.getMessage)))
      InternalServerError(views.html.global_error(Messages("global.error.InternalServerError500.title"),
        Messages("global.error.InternalServerError500.heading"),
        Messages("global.error.InternalServerError500.message"))
      )
    }
  }

  override def onHandlerNotFound(request: RequestHeader) = {
    implicit val hc = HeaderCarrier(request)
    Future.successful {
      auditConnector.audit(createAuditEvent("ServerValidationError", "Resource Endpoint Not Found", request, hc))
      NotFound(views.html.global_error(Messages("global.error.pageNotFound404.title"),
        Messages("global.error.pageNotFound404.heading"),
        Messages("global.error.pageNotFound404.message"))
      )
    }
  }

  override def onBadRequest(request: RequestHeader, error: String) = {
    implicit val hc = HeaderCarrier(request)
    Future.successful {
      auditConnector.audit(createAuditEvent("ServerValidationError", "Request bad format exception", request, hc))
      BadRequest(
        views.html.global_error(Messages("global.error.badRequest400.title"),
          Messages("global.error.badRequest400.heading"),
          Messages("global.error.badRequest400.message"))
      )
    }
  }

  private def createAuditEvent(eventType: String, transactionName: String, request: RequestHeader, hc: HeaderCarrier, errorMessage: Option[String] = None) = {
    val (details, tags) = buildAuditData(request, hc, transactionName)
    val errorMap = errorMessage.map(message =>  Map("transactionFailureReason" -> message)).getOrElse(Map())
    AuditEvent(auditType = eventType,
      detail = details ++ errorMap,
      tags = tags)
  }

  private def buildAuditData(request: RequestHeader, hc: HeaderCarrier, transactionName: String) = {
    val details = Map[String, String](
      "input" -> s"Request to ${request.path}",
      "ipAddress" -> hc.forwarded.getOrElse("-"),
      "method" -> request.method.toUpperCase,
      "userAgentString" -> request.headers.get("User-Agent").getOrElse("-"),
      "referrer" -> request.headers.get("Referer").getOrElse("-"))

    val tags = Map[String, String](
      "X-Request-ID" -> hc.requestId.getOrElse("-"),
      "X-Session-ID" -> hc.sessionId.getOrElse("-"),
      "transactionName" -> transactionName,
      "path" -> request.path
    )

    (details, tags)
  }
}
