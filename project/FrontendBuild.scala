import play.sbt.routes.RoutesKeys.routesImport
import play.sbt.PlayImport._
import sbt._

import scala.util.Properties._

object FrontendBuild extends Build with MicroService {

  import Dependencies._

  val appName = "preferences-frontend"
  val appVersion = envOrElse("PREFERENCES_FRONTEND_VERSION", "999-SNAPSHOT")

  override lazy val appDependencies = requiredDependencies

  override lazy val playSettings = Seq(routesImport ++= Seq(
    "uk.gov.hmrc.domain._"
  ))
}

private object Dependencies {

  val requiredDependencies = Seq(
    ws,
    "uk.gov.hmrc"             %% "a-b-test"                 % "2.0.0",
    "uk.gov.hmrc"             %% "emailaddress"             % "2.0.0",
    "uk.gov.hmrc"             %% "frontend-bootstrap"       % "7.15.0",
    "uk.gov.hmrc"             %% "govuk-template"           % "5.1.0",
    "uk.gov.hmrc"             %% "play-authorisation"       % "4.3.0",
    "uk.gov.hmrc"             %% "play-authorised-frontend" % "6.3.0",
    "uk.gov.hmrc"             %% "play-config"              % "4.2.0",
    "uk.gov.hmrc"             %% "play-health"              % "2.1.0",
    "uk.gov.hmrc"             %% "logback-json-logger"      % "3.1.0",
    "uk.gov.hmrc"             %% "play-ui"                  % "7.0.0",
    "com.netaporter"          %% "scala-uri"                % "0.4.14",
    "uk.gov.hmrc"             %% "url-builder"              % "2.1.0",
    "uk.gov.hmrc"             %% "reactive-circuit-breaker" % "2.0.0",
    "org.mockito"             %  "mockito-all"              % "1.10.19"   % "test",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "1.5.1"     % "test, it",


    "uk.gov.hmrc"             %% "http-verbs-test"          % "1.1.0"     % "it",
    "uk.gov.hmrc"             %% "auth-test"                % "4.1.0"     % "test, it",
    "uk.gov.hmrc"             %% "hmrctest"                 % "2.3.0"     % "test, it",
    "org.jsoup"               %  "jsoup"                    % "1.8.3"     % "test, it",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"     % "test, it",
    "org.scalatest"           %% "scalatest"                % "2.2.6"     % "test, it, functional",

    "uk.gov.hmrc"             %% "browser-test"             % "1.9.0"     % "functional",
    "com.github.tomakehurst"  %  "wiremock"                 % "2.1.11"    % "functional"
  )
}
