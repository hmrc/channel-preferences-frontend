import sbt._
import Keys._
import play.Project._
import net.litola.SassPlugin
import scala._
import com.gu.SbtJasminePlugin._

object GovUkTaxBuild extends Build {

  val appName = "govuk-tax"

  val appDependencies = Seq(
    Dependencies.Compile.nscalaTime,
    Dependencies.Compile.json4sExt,
    Dependencies.Compile.json4sJackson,
    Dependencies.Compile.guava,
    Dependencies.Compile.commonsLang,
    Dependencies.Compile.commonsIo,
    Dependencies.Compile.playMetrics,
    Dependencies.Compile.metricsGraphite,
    Dependencies.Compile.secureUtils,
    Dependencies.Compile.taxCore,

    Dependencies.Test.junit,
    Dependencies.Test.scalaTest,
    Dependencies.Test.mockito,
    Dependencies.Test.jsoup
  )

  val allPhases = "tt->test;test->test;test->compile;compile->compile"

  def templateSpecFilter(name: String): Boolean = name endsWith "TemplateSpec"

  lazy val TemplateTest = config("tt") extend(Test)

  val configPath = "-Dconfig.file=../../conf/application.conf"

  val common = play.Project(
    appName + "-common", Version.thisApp, appDependencies, file("modules/common"),
    settings = Common.commonSettings ++ SassPlugin.sassSettings
  ).configs(TemplateTest)
   .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
   .settings(testOptions in TemplateTest := Seq(Tests.Filter(templateSpecFilter)))
   .settings(javaOptions in Test += configPath)

  val paye = play.Project(
    appName + "-paye", Version.thisApp, appDependencies, path = file("modules/paye"), settings = Common.commonSettings
  ).dependsOn(common % allPhases)
    .configs(TemplateTest)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .settings(testOptions in TemplateTest := Seq(Tests.Filter(templateSpecFilter)))
    .settings(javaOptions in Test += configPath)
    .settings(jasmineTestDir <+= baseDirectory { src => src / "test" / "views" / "paye" })

  val agent = play.Project(
    appName + "-agent", Version.thisApp, appDependencies, path = file("modules/agent"), settings = Common.commonSettings
  ).dependsOn(common % allPhases)
    .configs(TemplateTest)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .settings(testOptions in TemplateTest := Seq(Tests.Filter(templateSpecFilter)))
    .settings(javaOptions in Test += configPath)


  val bt = play.Project(
    appName + "-business-tax", Version.thisApp, appDependencies, path = file("modules/business-tax"), settings = Common.commonSettings
  ).dependsOn(common % allPhases)
    .configs(TemplateTest)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .settings(testOptions in TemplateTest := Seq(Tests.Filter(templateSpecFilter)))
    .settings(javaOptions in Test += configPath)

  val sa = play.Project(
    appName + "-sa", Version.thisApp, appDependencies, path = file("modules/sa"), settings = Common.commonSettings
  ).dependsOn(common % allPhases, bt)
    .configs(TemplateTest)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .settings(testOptions in TemplateTest := Seq(Tests.Filter(templateSpecFilter)))
    .settings(javaOptions in Test += configPath)

  lazy val govukTax = play.Project(
    appName,
    Version.thisApp, appDependencies,
    settings = Common.commonSettings ++ SassPlugin.sassSettings
  ).settings(publishArtifact := true).dependsOn(paye, agent, sa, bt).aggregate(common, paye, agent, sa, bt)

}

object Common {
  val commonSettings = Defaults.defaultSettings  ++
    Seq(
      organization := "uk.gov.hmrc",
      version := Version.thisApp,
      scalaVersion := Version.scala,
      scalacOptions ++= Seq(
        "-unchecked",
        "-deprecation",
        "-Xlint",
	      "-Xmax-classfile-name", "100",
        "-language:_",
        "-target:jvm-1.7",
        "-encoding", "UTF-8"
      ),

      resolvers ++= Repositories.resolvers,
      retrieveManaged := true,
      testOptions in Test += Tests.Argument("-u", "target/test-reports")
    ) ++
    jasmineSettings ++
    Seq(
      appJsDir <+= baseDirectory {dir => dir},
      appJsLibDir <+= baseDirectory {dir => dir / ".." / "common" / "public" / "javascripts" / "vendor" },
      jasmineConfFile <+= baseDirectory { src => src / ".." / "common" / "test" / "views" / "test.dependencies.js"},
      (test in Test) <<= (test in Test) dependsOn (jasmine)
    ) ++ playScalaSettings ++ Repositories.publishingSettings
}
