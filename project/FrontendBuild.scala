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
    "uk.gov.hmrc"             %% "emailaddress"             % "1.0.0",
    "uk.gov.hmrc"             %% "frontend-bootstrap"       % "5.1.1",
    "uk.gov.hmrc"             %% "govuk-template"           % "2.6.0",
    "uk.gov.hmrc"             %% "play-authorisation"       % "3.1.0",
    "uk.gov.hmrc"             %% "play-authorised-frontend" % "4.5.0",
    "uk.gov.hmrc"             %% "play-config"              % "2.0.1",
    "uk.gov.hmrc"             %% "play-health"              % "1.1.0",
    "uk.gov.hmrc"             %% "play-json-logger"         % "2.1.1",
    "uk.gov.hmrc"             %% "play-ui"                  % "4.2.0",
    "com.netaporter"          %% "scala-uri"                % "0.4.10",
    "uk.gov.hmrc"             %% "url-builder"              % "1.0.0",

    "org.mockito"             %  "mockito-all"              % "1.10.19"   % "test",

    "uk.gov.hmrc"             %% "http-verbs-test"          % "0.1.0"     % "it",
    "uk.gov.hmrc"             %% "browser-test"             % "1.0.0"     % "it",
    "com.github.tomakehurst"  %  "wiremock"                 % "1.58"      % "it",

    "uk.gov.hmrc"             %% "auth-test"                % "2.1.0"     % "test, it",
    "uk.gov.hmrc"             %% "hmrctest"                 % "1.4.0"     % "test, it",
    "org.jsoup"               %  "jsoup"                    % "1.8.3"     % "test, it",
    "org.pegdown"             %  "pegdown"                  % "1.4.2"     % "test, it",
    "org.scalatest"           %% "scalatest"                % "2.2.5"     % "test, it"
  )
}
