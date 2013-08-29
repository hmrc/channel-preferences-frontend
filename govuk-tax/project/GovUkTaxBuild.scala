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
    Dependencies.Test.mockito,
    Dependencies.Test.jsoup
  )

  val allPhases = "tt->test;test->test;test->compile;compile->compile"

  def templateSpecFilter(name: String): Boolean = name endsWith "TemplateSpec"

  lazy val TemplateTest = config("tt") extend(Test)

  val common = play.Project(
    appName + "-common", Version.thisApp, appDependencies, file("modules/common"),
    settings = Common.commonSettings ++ SassPlugin.sassSettings
  ).configs(TemplateTest)
   .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
   .settings(testOptions in TemplateTest := Seq(Tests.Filter(templateSpecFilter)))

  val paye = play.Project(
    appName + "-paye", Version.thisApp, appDependencies, path = file("modules/paye"), settings = Common.commonSettings
  ).dependsOn(common % allPhases)
    .configs(TemplateTest)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .settings(testOptions in TemplateTest := Seq(Tests.Filter(templateSpecFilter)))

  val agent = play.Project(
    appName + "-agent", Version.thisApp, appDependencies, path = file("modules/agent"), settings = Common.commonSettings
  ).dependsOn(common % allPhases)
    .configs(TemplateTest)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .settings(testOptions in TemplateTest := Seq(Tests.Filter(templateSpecFilter)))


  val bt = play.Project(
    appName + "-business-tax", Version.thisApp, appDependencies, path = file("modules/business-tax"), settings = Common.commonSettings
  ).dependsOn(common % allPhases)
    .configs(TemplateTest)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .settings(testOptions in TemplateTest := Seq(Tests.Filter(templateSpecFilter)))

  val sa = play.Project(
    appName + "-sa", Version.thisApp, appDependencies, path = file("modules/sa"), settings = Common.commonSettings
  ).dependsOn(common % allPhases, bt)
    .configs(TemplateTest)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .settings(testOptions in TemplateTest := Seq(Tests.Filter(templateSpecFilter)))

  lazy val govukTax = play.Project(
    appName,
    Version.thisApp, appDependencies,
    settings = Common.commonSettings ++ SassPlugin.sassSettings
  ).settings(publishArtifact := true).dependsOn(paye, agent, sa, bt).aggregate(common, paye, agent, sa, bt)

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
