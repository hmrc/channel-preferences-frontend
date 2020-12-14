import play.sbt.PlayImport.ws
import sbt.Tests.{Group, SubProcess}
import sbt.{ForkOptions, TestDefinition, _}

object Dependencies {
  val requiredDependencies = Seq(
    ws,
    "uk.gov.hmrc"            %% "a-b-test"                 % "3.2.0",
    "uk.gov.hmrc"            %% "emailaddress"             % "3.5.0",
    "uk.gov.hmrc"            %% "bootstrap-play-26"        % "2.2.0",
    "uk.gov.hmrc"            %% "metrix"                   % "4.7.0-play-26",
    "uk.gov.hmrc"            %% "play-ui"                  % "8.12.0-play-26",
    "uk.gov.hmrc"            %% "govuk-template"           % "5.54.0-play-26",
    "uk.gov.hmrc"            %% "play-frontend-govuk"      % "0.50.0-play-26",
    "uk.gov.hmrc"            %% "play-frontend-hmrc"       % "0.19.0-play-26",
    "org.webjars.npm"        % "govuk-frontend"            % "3.7.0",
    "com.typesafe.play"      %% "play-json-joda"           % "2.6.13",
    "uk.gov.hmrc"            %% "auth-client"              % "3.0.0-play-26",
    "com.netaporter"         %% "scala-uri"                % "0.4.16",
    "uk.gov.hmrc"            %% "domain"                   % "5.9.0-play-26",
    "uk.gov.hmrc"            %% "url-builder"              % "3.4.0-play-26",
    "uk.gov.hmrc"            %% "reactive-circuit-breaker" % "3.4.0",
    "com.beachape"           %% "enumeratum"               % "1.6.0",
    "com.beachape"           %% "enumeratum-play-json"     % "1.6.0",
    "org.mockito"            % "mockito-all"               % "1.10.19" % "test",
    "org.scalatestplus.play" %% "scalatestplus-play"       % "3.1.3" % "test, it",
    "org.jsoup"              % "jsoup"                     % "1.13.1" % "test, it",
    "org.pegdown"            % "pegdown"                   % "1.6.0" % "test, it",
    "uk.gov.hmrc"            %% "browser-test"             % "2.3.0" % "functional",
    "com.github.tomakehurst" % "wiremock"                  % "2.26.3" % "functional"
  )

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
    tests map { test =>
      Group(test.name, Seq(test), runPolicy =  SubProcess(ForkOptions().withRunJVMOptions(Vector("-Dtest.name=" + test.name))))
    }
}
