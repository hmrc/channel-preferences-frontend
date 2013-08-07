import sbt._
import Keys._
import play.Project._
import net.litola.SassPlugin
import com.typesafe.sbt.SbtScalariform._

object GovUkTaxBuild extends Build {

  val appName = "govuk-tax"

  val appDependencies = Seq(
    anorm,
    Dependencies.Compile.nscalaTime,
    Dependencies.Compile.json4sExt,
    Dependencies.Compile.json4sJackson,
    Dependencies.Compile.guava,
    Dependencies.Compile.playMetrics,
    Dependencies.Compile.metricsGraphite,
    Dependencies.Compile.secureUtils,

    Dependencies.Test.junit,
    Dependencies.Test.scalaTest,
    Dependencies.Test.mockito
  )

  lazy val govukTax = play.Project(
    appName,
    Version.thisApp, appDependencies,
    settings = Common.commonSettings ++ SassPlugin.sassSettings
  ).settings(publishArtifact := true)

}

object Common {
  val commonSettings = Defaults.defaultSettings ++
    scalariformSettings ++
    Seq(
      organization := "uk.gov.hmrc",
      version := Version.thisApp,
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
      retrieveManaged := true,
      testOptions in Test <+= (target in Test) map {
        t => Tests.Argument(TestFrameworks.ScalaTest, "junitxml(directory=\"%s\")" format (t / "test-reports"))
      }
    ) ++
    Repositories.publishingSettings
}
