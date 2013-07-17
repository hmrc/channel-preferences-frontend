package config

import play.api.{ Application, Logger }
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits._
import java.text.SimpleDateFormat
import java.util.Date
import com.kenshoo.play.metrics.MetricsFilter
import com.codahale.metrics.graphite.{ GraphiteReporter, Graphite }
import java.net.InetSocketAddress
import com.codahale.metrics.{ MetricFilter, SharedMetricRegistries, MetricRegistry }
import java.util.concurrent.TimeUnit

object AccessLoggingFilter extends Filter {
  def apply(next: (RequestHeader) => Result)(rh: RequestHeader) = {
    val start = System.currentTimeMillis
    val startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZZZ").format(new Date(start))

    def logTime(result: PlainResult): Result = {
      val time = System.currentTimeMillis - start
      // Apache combined log format http://httpd.apache.org/docs/2.4/logs.html
      Logger("accesslog").info(s"${rh.remoteAddress} - ${rh.session.get("username").getOrElse("-")} " +
        s"[$startTime] '${rh.method} ${rh.uri}' ${result.header.status} - ${time}ms " +
        s"'${rh.headers.get("Referer").getOrElse("-")}' '${rh.headers.get("User-Agent").getOrElse("-")}'")
      result
    }

    next(rh) match {
      case plain: PlainResult => logTime(plain)
      case async: AsyncResult => async.transform(logTime)
    }
  }
}

object Global extends WithFilters(MetricsFilter, AccessLoggingFilter) {

  override def onStart(app: Application) {
    val env = app.mode
    if (app.configuration.getBoolean("metrics.enabled").getOrElse(false) &&
      app.configuration.getBoolean(s"govuk-tax.$env.metrics.graphite.enabled").getOrElse(false)) {
      startGraphite(app)
    }
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
}
