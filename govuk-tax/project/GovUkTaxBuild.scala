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

  val allPhases = "test->test;test->compile;compile->compile"

  val common = play.Project(
    appName + "-common", Version.thisApp, appDependencies, file("modules/common"),
    settings = Common.commonSettings ++ SassPlugin.sassSettings
  )

  val paye = play.Project(
    appName + "-paye", Version.thisApp, appDependencies, path = file("modules/paye")
  ).dependsOn(common % allPhases)

  val agent = play.Project(
    appName + "-agent", Version.thisApp, appDependencies, path = file("modules/agent")
  ).dependsOn(common % allPhases)

  val sa = play.Project(
    appName + "-sa", Version.thisApp, appDependencies, path = file("modules/sa")
  ).dependsOn(common % allPhases)

  val bt = play.Project(
    appName + "-business-tax", Version.thisApp, appDependencies, path = file("modules/business-tax")
  ).dependsOn(common % allPhases)


  lazy val govukTax = play.Project(
    appName,
    Version.thisApp, appDependencies,
    settings = Common.commonSettings ++ SassPlugin.sassSettings
  ).settings(publishArtifact := true).dependsOn(paye, agent, sa, bt).aggregate(paye, agent, sa, bt)

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
