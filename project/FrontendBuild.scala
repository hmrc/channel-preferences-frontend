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
    "uk.gov.hmrc"             %% "a-b-test"                 % "2.0.0",
    "uk.gov.hmrc"             %% "emailaddress"             % "3.2.0",
    "uk.gov.hmrc"             %% "frontend-bootstrap"       % "12.8.0",
    "uk.gov.hmrc"             %% "auth-client"              % "2.6.0",
    "com.netaporter"          %% "scala-uri"                % "0.4.16",
    "uk.gov.hmrc"             %% "url-builder"              % "3.1.0",
    "uk.gov.hmrc"             %% "reactive-circuit-breaker" % "3.3.0",
    "org.mockito"             %  "mockito-all"              % "1.10.19"   % "test",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "2.0.1"     % "test, it",
    "uk.gov.hmrc"             %% "http-verbs-test"          % "1.6.0-play-25"     % "it",
    "uk.gov.hmrc"             %% "auth-test"                % "5.3.0"     % "test, it",
    "uk.gov.hmrc"             %% "hmrctest"                 % "3.9.0-play-25"     % "test, it",
    "org.jsoup"               %  "jsoup"                    % "1.12.1"     % "test, it",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"     % "test, it",
    "org.scalatest"           %% "scalatest"                % "3.0.7"     % "test, it, functional",

    "uk.gov.hmrc"             %% "browser-test"             % "1.10.0"    % "functional",
    "com.github.tomakehurst"  %  "wiremock"                 % "2.1.11"    % "functional"
  )
}
