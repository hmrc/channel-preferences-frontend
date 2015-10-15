import play.PlayImport.PlayKeys._
import play.PlayImport._
import sbt._

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
  private val scalatestVersion = "2.2.2"
  private val mockitoVersion = "1.9.5"
  private val pegdownVersion = "1.4.2"
  private val jsoupVersion = "1.7.3"


  val requiredDependencies = Seq(
    ws,
    "uk.gov.hmrc"    %% "govuk-template"           % "2.6.0",
    "uk.gov.hmrc"    %% "play-authorisation"       % "0.12.0",
    "uk.gov.hmrc"    %% "play-health"              % "0.7.0",
    "uk.gov.hmrc"    %% "emailaddress"             % "0.2.0",
    "uk.gov.hmrc"    %% "url-builder"              % "0.8.0",
    "uk.gov.hmrc"    %% "crypto"                   % "1.6.0",
    "uk.gov.hmrc"    %% "play-ui"                  % "1.8.1",
    "uk.gov.hmrc"    %% "a-b-test"                 % "0.2.0",
    "com.netaporter" %% "scala-uri"                % "0.4.2",
    "uk.gov.hmrc"    %% "play-authorised-frontend" % "1.2.0",
    "uk.gov.hmrc"    %% "play-config"              % "1.0.0",
    "uk.gov.hmrc"    %% "play-json-logger"         % "1.0.0",
    "uk.gov.hmrc"    %% "http-verbs"               % "1.6.0",
    "uk.gov.hmrc"    %% "frontend-bootstrap"       % "0.8.0",

    "org.jsoup"      %  "jsoup"       % jsoupVersion     % "test",
    "org.scalatest"  %% "scalatest"   % scalatestVersion % "test",
    "org.pegdown"    %  "pegdown"     % pegdownVersion   % "test",
    "org.mockito"    %  "mockito-all" % mockitoVersion   % "test",
    "uk.gov.hmrc"    %% "hmrctest"    % "1.3.0"          % "test",
    "uk.gov.hmrc"    %% "auth-test"   % "0.8.0"          % "test",

    "org.scalatest"  %% "scalatest"   % scalatestVersion % "it",
    "org.pegdown"    %  "pegdown"     % pegdownVersion   % "it",
    "org.jsoup"      %  "jsoup"       % jsoupVersion     % "it",
    "uk.gov.hmrc"    %% "hmrctest"    % "1.3.0"          % "it",
    "uk.gov.hmrc"    %% "auth-test"   % "0.9.0"          % "it"
  )
}
