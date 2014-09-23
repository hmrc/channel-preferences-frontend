import play.PlayImport._
import play.PlayImport.PlayKeys._
import sbt._
import scala._
import scala.util.Properties._

object FrontendBuild extends Build with MicroService {

  import Dependencies._

  val appName = "preferences-frontend"
  val appVersion = envOrElse("PREFERENCES_FRONTEND_VERSION", "999-SNAPSHOT")

  override lazy val appDependencies = requiredDependencies

  override lazy val playSettings = Seq(routesImport ++= Seq(
    "controllers.sa.prefs._",
    "uk.gov.hmrc.domain._"
  ))
}

private object Dependencies {

  private val metricsGraphiteVersion = "3.0.1"
  private val playMetricsVersion = "0.1.3"
  val requiredDependencies = Seq(
    ws,
//    "com.kenshoo" %% "metrics-play" % playMetricsVersion,
//    "com.codahale.metrics" % "metrics-graphite" % metricsGraphiteVersion,
    "uk.gov.hmrc"    %% "govuk-template" % "2.0.1",
    "uk.gov.hmrc"    %% "play-frontend"  % "8.3.0",
    "uk.gov.hmrc"    %% "play-health"    % "0.5.0",
    "uk.gov.hmrc"    %% "emailaddress"   % "0.2.0",
    "uk.gov.hmrc"    %% "url-builder"    % "0.2.0",
    "com.netaporter" %% "scala-uri"      % "0.4.2",

    "org.jsoup"      %  "jsoup"       % "1.7.2"  % "test",
    "org.scalatest"  %% "scalatest"   % "2.2.0"  % "test",
    "org.pegdown"    %  "pegdown"     % "1.4.2"  % "test",
    "org.mockito"    %  "mockito-all" % "1.9.5"  % "test"
  )
}
