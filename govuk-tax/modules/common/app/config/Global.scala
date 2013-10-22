package config

import play.api.GlobalSettings

//import play.api.{ Application, Logger }
//import play.api.mvc._
//import scala.concurrent.ExecutionContext.Implicits._
//import java.text.SimpleDateFormat
//import java.util.Date
//import com.kenshoo.play.metrics.MetricsFilter
//import com.codahale.metrics.graphite.{ GraphiteReporter, Graphite }
//import java.net.InetSocketAddress
//import com.codahale.metrics.{ MetricFilter, SharedMetricRegistries }
//import java.util.concurrent.TimeUnit
//import play.api.mvc.AsyncResult

//object Global extends WithFilters(MetricsFilter) {
object Global extends GlobalSettings {

//  override def onStart(app: Application) {
//    val env = app.mode
//    if (app.configuration.getBoolean("metrics.enabled").getOrElse(false) &&
//      app.configuration.getBoolean(s"govuk-tax.$env.metrics.graphite.enabled").getOrElse(false)) {
//      startGraphite(app)
//    }
//  }

//  def startGraphite(app: Application) {
//    val env = app.mode
//
//    val graphite = new Graphite(new InetSocketAddress(
//      app.configuration.getString(s"govuk-tax.$env.metrics.graphite.host").getOrElse("graphite"),
//      app.configuration.getInt(s"govuk-tax.$env.metrics.graphite.port").getOrElse(2003)))
//
//    val prefix = app.configuration.getString(s"govuk-tax.$env.metrics.graphite.prefix").getOrElse("tax")
//
//    val reporter = GraphiteReporter.forRegistry(
//      SharedMetricRegistries.getOrCreate(app.configuration.getString("metrics.name").getOrElse("default")))
//      .prefixedWith(s"$prefix.${java.net.InetAddress.getLocalHost.getHostName}")
//      .convertRatesTo(TimeUnit.SECONDS)
//      .convertDurationsTo(TimeUnit.MILLISECONDS)
//      .filter(MetricFilter.ALL)
//      .build(graphite)
//
//    reporter.start(app.configuration.getLong(s"govuk-tax.$env.metrics.graphite.interval").getOrElse(10L), TimeUnit.SECONDS)
//  }


}
