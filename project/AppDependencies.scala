/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "com.beachape"      %% "enumeratum"                 % "1.6.1",
    "com.beachape"      %% "enumeratum-play-json"       % "1.6.1",
    "com.iheart"        %% "play-swagger"               % "0.10.2",
    "com.iterable"      %% "swagger-play"               % "2.0.1",
    "com.netaporter"    %% "scala-uri"                  % "0.4.16",
    "com.typesafe.play" %% "play-json-joda"             % "2.6.13",
    "org.typelevel"     %% "cats-core"                  % "2.3.1",
    "org.webjars.npm"    % "govuk-frontend"             % "3.10.2",
    "uk.gov.hmrc"       %% "auth-client"                % "4.0.0-play-27",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-27" % "3.4.0",
    "uk.gov.hmrc"       %% "domain"                     % "5.10.0-play-27",
    "uk.gov.hmrc"       %% "emailaddress"               % "3.5.0",
    "uk.gov.hmrc"       %% "govuk-template"             % "5.61.0-play-27",
    "uk.gov.hmrc"       %% "metrix"                     % "4.7.0-play-27",
    "uk.gov.hmrc"       %% "play-frontend-govuk"        % "0.60.0-play-27",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "0.40.0-play-27",
    "uk.gov.hmrc"       %% "play-language"              % "4.10.0-play-27",
    "uk.gov.hmrc"       %% "play-ui"                    % "8.21.0-play-27",
    "uk.gov.hmrc"       %% "reactive-circuit-breaker"   % "3.5.0",
    "uk.gov.hmrc"       %% "url-builder"                % "3.4.0-play-27"
  )

  val test = Seq(
    "com.github.tomakehurst" % "wiremock-jre8"           % "2.27.2"  % "test,it",
    "com.typesafe.play"     %% "play-test"               % current   % Test,
    "com.vladsch.flexmark"   % "flexmark-all"            % "0.36.8"  % "test, it",
    "org.jsoup"              % "jsoup"                   % "1.13.1"  % "test, it",
    "org.mockito"            % "mockito-all"             % "1.10.19" % "test",
    "org.pegdown"             % "pegdown"                % "1.6.0"   % "test, it",
    "org.scalatest"          %% "scalatest"              % "3.0.0"   % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"     % "4.0.3"   % "test, it",
    "uk.gov.hmrc"            %% "bootstrap-test-play-27" % "3.4.0"   % Test
  )

  val dependencyOverrides = Seq(
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.8" // swagger requires an older version of jackson than alpakka...
  )
}
