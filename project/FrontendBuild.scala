import play.sbt.routes.RoutesKeys.routesImport
import play.sbt.PlayImport._
import sbt._

import scala.util.Properties._

object FrontendBuild extends Build with MicroService {

  import Dependencies._

  val appName = "preferences-frontend"

  override lazy val appDependencies = requiredDependencies

  override lazy val playSettings = Seq(routesImport ++= Seq(
    "uk.gov.hmrc.domain._"
  ))
}

private object Dependencies {

  val requiredDependencies = Seq(
    ws,
    "uk.gov.hmrc"             %% "a-b-test"                 % "3.2.0",
    "uk.gov.hmrc"             %% "emailaddress"             % "3.4.0",
    "uk.gov.hmrc"             %% "bootstrap-play-26"        % "1.7.0",
    "uk.gov.hmrc"             %% "metrix"                   % "4.4.0-play-26",
    "uk.gov.hmrc"             %% "play-ui"                  % "8.9.0-play-26",
    "uk.gov.hmrc"             %% "govuk-template"           % "5.54.0-play-26",
    "uk.gov.hmrc"             %% "play-frontend-govuk"      % "0.43.0-play-26",
    "org.webjars.npm"         %  "govuk-frontend"           % "3.1.0",
    "com.typesafe.play"       %% "play-json-joda"           % "2.6.13",
    "uk.gov.hmrc"             %% "auth-client"              % "3.0.0-play-26",
    "com.netaporter"          %% "scala-uri"                % "0.4.16",
    "uk.gov.hmrc"             %% "domain"                   % "5.8.0-play-26",
    "uk.gov.hmrc"             %% "url-builder"              % "3.3.0-play-26",
    "uk.gov.hmrc"             %% "reactive-circuit-breaker" % "3.4.0",
    "com.beachape"            %% "enumeratum"               % "1.5.13",
    "com.beachape"            %% "enumeratum-play-json"     % "1.5.13",
    "org.mockito"             %  "mockito-all"              % "1.10.19"   % "test",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "3.1.0"     % "test, it",
    "org.jsoup"               %  "jsoup"                    % "1.12.1"     % "test, it",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"     % "test, it",
    "uk.gov.hmrc"             %% "browser-test"             % "2.3.0"    % "functional",
    "com.github.tomakehurst"  %  "wiremock"                 % "2.18.0"    % "functional"
  )
}
