import sbt._
import Keys._
import play.Project._
import net.litola.SassPlugin
import com.typesafe.sbt.SbtScalariform._

object ApplicationBuild extends Build {

  val appName = "govuk-tax"

  val commonSettings = Defaults.defaultSettings ++
    scalariformSettings ++
    Seq(
      organization := "uk.gov.hmrc",
      scalaVersion := Version.scala,
      scalacOptions ++= Seq(
        "-unchecked",
        "-deprecation",
        "-Xlint",
        "-language:_",
        "-target:jvm-1.7",
        "-encoding", "UTF-8"
      ),
      resolvers ++= Repositories.resolvers,
      retrieveManaged := true
    )

  val appDependencies = Seq(
    anorm,
    Dependency.Compile.nscalaTime,
    Dependency.Compile.json4sExt,
    Dependency.Compile.json4sJackson,
    Dependency.Compile.guava,

    Dependency.Test.junit,
    Dependency.Test.scalaTest,
    Dependency.Test.mockito
  )

  lazy val playSpike = play.Project(
    appName,
    Version.thisPlayApp, appDependencies,
    settings = commonSettings ++ SassPlugin.sassSettings)


  object Version {
    val thisPlayApp = "0.0.1-SNAPSHOT"
    val scala = "2.10.1"
  }

  object Dependency {

    object Compile {
      val nscalaTime = "com.github.nscala-time" %% "nscala-time" % "0.4.0"
      val json4sJackson = "org.json4s" %% "json4s-jackson" % "3.2.4"
      val json4sExt = "org.json4s" %% "json4s-ext" % "3.2.4"
      val guava = "com.google.guava" % "guava" % "14.0.1"
    }

    object Test {
      val junit = "junit" % "junit" % "4.11" % "test"
      val scalaTest = "org.scalatest" %% "scalatest" % "2.0.M5b" % "test"
      val mockito = "org.mockito" % "mockito-all" % "1.9.5" % "test"
      val scalaMock = "org.scalamock" %% "scalamock-scalatest-support" % "3.1-SNAPSHOT" % "test"
      val junitdep = "junit" % "junit-dep" % "4.11" % "test"
    }
  }

  object Repositories {
    val hmrcNexusReleases = "hmrc-releases" at "http://nexus-preview.tax.service.gov.uk/content/repositories/hmrc-releases"
    val hmrcNexusSnapshots = "hmrc-snapshots" at "http://nexus-preview.tax.service.gov.uk/content/repositories/hmrc-snapshots"
    val hmrcThirdpartyReleases = "thirdparty-releases" at "http://nexus-preview.tax.service.gov.uk/content/repositories/thirdparty-releases"
    val hmrcThirdpartySnapshots = "thirdparty-snapshots" at "http://nexus-preview.tax.service.gov.uk/content/repositories/thirdparty-snapshots"

    val resolvers = Seq(
      hmrcNexusReleases,
      hmrcNexusSnapshots,
      hmrcThirdpartyReleases,
      hmrcThirdpartySnapshots,
      Opts.resolver.sonatypeReleases,
      Opts.resolver.sonatypeSnapshots
    )
  }

}

