import play.PlayImport.PlayKeys._
import play.PlayImport._
import sbt.Keys._
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
    "uk.gov.hmrc"             %% "a-b-test"                 % "1.0.0",
    "uk.gov.hmrc"             %% "emailaddress"             % "1.1.0",
    "uk.gov.hmrc"             %% "frontend-bootstrap"       % "6.7.0",
    "uk.gov.hmrc"             %% "govuk-template"           % "4.0.0",
    "uk.gov.hmrc"             %% "play-authorisation"       % "3.3.0",
    "uk.gov.hmrc"             %% "play-authorised-frontend" % "5.5.0",
    "uk.gov.hmrc"             %% "play-config"              % "2.1.0",
    "uk.gov.hmrc"             %% "play-health"              % "1.1.0",
    "uk.gov.hmrc"             %% "play-json-logger"         % "2.1.1",
    "uk.gov.hmrc"             %% "play-ui"                  % "4.16.0",
    "com.netaporter"          %% "scala-uri"                % "0.4.14",
    "uk.gov.hmrc"             %% "url-builder"              % "1.1.0",
    "uk.gov.hmrc"             %% "reactive-circuit-breaker" % "1.7.0",
    "org.mockito"             %  "mockito-all"              % "1.10.19"   % "test",

    "uk.gov.hmrc"             %% "http-verbs-test"          % "0.1.0"     % "it",
    "uk.gov.hmrc"             %% "auth-test"                % "2.4.0"     % "test, it",
    "uk.gov.hmrc"             %% "hmrctest"                 % "1.8.0"     % "test, it",
    "org.jsoup"               %  "jsoup"                    % "1.8.3"     % "test, it",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"     % "test, it",
    "org.scalatest"           %% "scalatest"                % "2.2.6"     % "test, it, functional",

    "uk.gov.hmrc"             %% "browser-test"             % "1.2.0"     % "functional",
    "com.github.tomakehurst"  %  "wiremock"                 % "2.1.11"    % "functional"
  )
}
