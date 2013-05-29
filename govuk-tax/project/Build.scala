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
      resolvers ++= Repositories.localAndSonatype,
      retrieveManaged := true,
      initialCommands in console := "import uk.gov.hmrc._"
    )

  val appDependencies = Seq(
    anorm,
    Dependency.Compile.nscalaTime,
    Dependency.Compile.json4sExt,
    Dependency.Compile.json4sJackson,

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
    val scala = "2.10.0"
  }

  object Dependency {

    object Compile {
      val nscalaTime = "com.github.nscala-time" %% "nscala-time" % "0.4.0"
      val json4sJackson = "org.json4s" %% "json4s-jackson" % "3.2.4"
      val json4sExt = "org.json4s" %% "json4s-ext" % "3.2.4"
    }

    object Test {
      val junit = "junit" % "junit" % "4.11" % "test"
      val scalaTest = "org.scalatest" %% "scalatest" % "2.0.M5b" % "test"
      val mockito = "org.mockito" % "mockito-all" % "1.9.5" % "test"
      val scalaMock = "org.scalamock" %% "scalamock-scalatest-support" % "3.1-SNAPSHOT" % "test"
    }
  }

  object Repositories {
    val localAndSonatype = Seq(Resolver.mavenLocal, Opts.resolver.sonatypeReleases, Opts.resolver.sonatypeSnapshots)
  }

}

