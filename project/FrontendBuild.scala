import sbt._
import scala._
import scala.util.Properties._
import uk.gov.hmrc.PlayMicroServiceBuild
import uk.gov.hmrc.HmrcResolvers
import uk.gov.hmrc.HmrcResolvers._

object FrontendBuild extends Build {

  import Dependencies._

  val appName = "preferences-frontend"
  val appVersion = envOrElse("PREFERENCES_FRONTEND_VERSION", "999-SNAPSHOT")

  val appSpecificDependencies = Seq("uk.gov.hmrc" %% "emailaddress" % "0.1.0", "com.netaporter" %% "scala-uri" % "0.4.0" exclude("com.typesafe.sbt", "sbt-pgp") exclude("com.github.scct", "scct_2.10"))
  val dependencies = requiredDependencies ++ appSpecificDependencies
  lazy val microservice = PlayMicroServiceBuild(appName,
    appVersion,
    dependencies,
    Seq("controllers.sa.prefs._", "uk.gov.hmrc.domain._"),
    applicationResolvers = HmrcResolvers(),
    snapshots = hmrcNexusSnapshots,
    releases = hmrcNexusReleases)
}

private object Dependencies {

  import play.Project._

  private val govukTemplateVersion = "2.0.1"
  private val playFrontendVersion = "8.1.0"

  private val metricsGraphiteVersion = "3.0.1"
  private val pegdownVersion = "1.4.2"
  private val playMetricsVersion = "0.1.3"
  private val playHealthVersion = "0.2.0"
  private val scalaTestVersion = "2.1.7"
  private val jsoupVersion = "1.7.2"
  private val mockitoVersion = "1.9.5"

  val requiredDependencies = Seq(
    filters,
    "com.kenshoo" %% "metrics-play" % playMetricsVersion,
    "com.codahale.metrics" % "metrics-graphite" % metricsGraphiteVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc" %% "play-frontend" % playFrontendVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,

    "org.jsoup" % "jsoup" % jsoupVersion % "test",
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
    "org.pegdown" % "pegdown" % pegdownVersion % "test",
    "org.mockito" % "mockito-all" % mockitoVersion % "test"
  )
}
