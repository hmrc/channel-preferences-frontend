import sbt._
import scala._
import scala.util.Properties._
import uk.gov.hmrc.PlayMicroServiceBuild

object FrontendBuild extends Build {

  import Dependencies._

  val appName = "preferences-frontend"
  val appVersion = envOrElse("PREFERENCES_FRONTEND_VERSION", "999-SNAPSHOT")

  val appSpecificDependencies = Seq("com.netaporter" %% "scala-uri" % "0.4.0" exclude("com.typesafe.sbt", "sbt-pgp") exclude("com.github.scct", "scct_2.10"))
  val dependencies = requiredDependencies ++ appSpecificDependencies
  lazy val microservice = PlayMicroServiceBuild(appName,
    appVersion,
    dependencies,
    Seq("uk.gov.hmrc.common.QueryBinders._", "uk.gov.hmrc.domain._"))
}

private object Dependencies {

  import play.Project._
  import uk.gov.hmrc.Dependency._

  private val govukTemplateVersion = "1.5.0"
  private val playFrontendVersion = "3.5.1"
  private val playMicroServiceVersion = "1.14.0"

  private val metricsGraphiteVersion = "3.0.1"
  private val pegdownVersion = "1.4.2"
  private val playMetricsVersion = "0.1.3"
  private val scalaTestVersion = "2.1.7"
  private val jsoupVersion = "1.7.2"
  private val mockitoVersion = "1.9.5"

  val govukTemplate = "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion
  val playFrontend = "uk.gov.hmrc" %% "play-frontend" % playFrontendVersion
  val playMicroservice = "uk.gov.hmrc" %% "play-microservice" % playMicroServiceVersion

  val metricsGraphite = "com.codahale.metrics" % "metrics-graphite" % metricsGraphiteVersion
  val pegdown = "org.pegdown" % "pegdown" % pegdownVersion
  val playMetrics = "com.kenshoo" %% "metrics-play" % playMetricsVersion
  val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion
  val jsoup = "org.jsoup" % "jsoup" % jsoupVersion
  val mockito = "org.mockito" % "mockito-all" % mockitoVersion

  val requiredDependencies = Seq(
    filters,
    compile(playMetrics),
    compile(metricsGraphite),
    compile(playMicroservice),
    compile(govukTemplate),
    compile(playFrontend),

    test(jsoup),
    test(scalaTest),
    test(pegdown),
    test(playMicroservice, Seq("tests")),
    test(mockito),

    integrationTest(scalaTest),
    integrationTest(pegdown),
    integrationTest(playMicroservice, Seq("tests", "tests-sources", "tests-javadoc"))
  )
}