import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import play.sbt.routes.RoutesKeys.routesGenerator
import play.routes.compiler.StaticRoutesGenerator
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import uk.gov.hmrc.{SbtBuildInfo, ShellPrompt}

  lazy val TemplateTest = config("tt") extend Test
  lazy val FunctionalTest = config("functional") extend Test
  lazy val TemplateItTest = config("tit") extend IntegrationTest

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
    tests map {
      test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }

  val appName: String
  val appVersion: String

  lazy val appDependencies : Seq[ModuleID] = ???
  lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala, SbtDistributablesPlugin)
  lazy val playSettings : Seq[Setting[_]] = Seq.empty

  lazy val microservice = Project(appName, file("."))
    .enablePlugins(plugins : _*)
    .settings(playSettings : _*)
    .settings(version := appVersion)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(
      scalacOptions ++= List(
        "-feature",
        "-Xlint",
        "-language:reflectiveCalls"
      )
    )
    .settings(
      targetJvm := "jvm-1.8",
      shellPrompt := ShellPrompt(appVersion),
      libraryDependencies ++= appDependencies,
      parallelExecution in Test := false,
      fork in Test := false,
      retrieveManaged := true,
      routesGenerator := StaticRoutesGenerator
    )
    .settings(inConfig(FunctionalTest)(Defaults.testSettings): _*)
    .configs(FunctionalTest)
    .settings(
      Keys.fork in FunctionalTest := false,
      testOptions in FunctionalTest := List(
        Tests.Argument(
          "-o", "-u", s"${target.value.getPath}/functional-test-reports", "-h", s"${target.value.getPath}/functional-test-reports/html-report"
        ),
        Tests.Setup(() => sys.props += "browser" -> "chrome")
      ),
      unmanagedSourceDirectories in FunctionalTest <<= (baseDirectory in FunctionalTest)(base => Seq(base / "functional")),
      parallelExecution in FunctionalTest := false
    )
    .settings(Repositories.playPublishingSettings : _*)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .configs(IntegrationTest)
    .settings(inConfig(TemplateItTest)(Defaults.itSettings): _*)
    .settings(
      Keys.fork in IntegrationTest := false,
      unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest)(base => Seq(base / "it")),
      addTestReportOption(IntegrationTest, "int-test-reports"),
      testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false)
    .settings(SbtBuildInfo(): _*)
    .settings(resolvers ++= Seq(Resolver.bintrayRepo("hmrc", "releases"), Resolver.jcenterRepo))
}

private object Repositories {

  import uk.gov.hmrc._
  import PublishingSettings._
  import NexusPublishing._

  lazy val playPublishingSettings : Seq[sbt.Setting[_]] = sbtrelease.ReleasePlugin.releaseSettings ++ Seq(

    credentials += SbtCredentials,

    publishArtifact in(Compile, packageDoc) := false,
    publishArtifact in(Compile, packageSrc) := false

  ) ++
    publishAllArtefacts ++
    nexusPublishingSettings

}