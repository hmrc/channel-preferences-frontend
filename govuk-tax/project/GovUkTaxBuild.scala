import sbt._
import Keys._
import play.Project._
import net.litola.SassPlugin
import scala._
import com.gu.SbtJasminePlugin._
import uk.gov.hmrc.{OptionalDependencies, MicroserviceDependencies}

object GovUkTaxBuild extends Build {

  val appName = "govuk-tax"

  val appDependencies = Seq(
    filters,
    anorm,
    MicroserviceDependencies.Compile.nscalaTime,
    MicroserviceDependencies.Compile.json4sJackson,
    MicroserviceDependencies.Compile.hmrc.taxCore,
    OptionalDependencies.Compile.hmrc.secureUtils,
    Dependencies.Compile.json4sExt,
    Dependencies.Compile.guava,
    Dependencies.Compile.commonsLang,
    Dependencies.Compile.commonsIo,
    Dependencies.Compile.playMetrics,
    Dependencies.Compile.metricsGraphite,

    MicroserviceDependencies.Test.junit,
    MicroserviceDependencies.Test.scalaTest,
    MicroserviceDependencies.Test.mockito,
    Dependencies.Test.jsoup,
    Dependencies.Test.pegdown
  )

  val allPhases = "tt->test;test->test;test->compile;compile->compile"

  def templateSpecFilter(name: String): Boolean = name endsWith "TemplateSpec"

  lazy val TemplateTest = config("tt") extend(Test)

  val configPath = "-Dconfig.file=../../conf/application.conf"
  val uiDirectory = SettingKey[File]("ui-directory")

  val govukTemplate = play.Project(
    appName + "-template", Version.thisApp, appDependencies, path = file("modules/govuk-template"), settings = Common.baseSettings
  ).settings(Keys.fork in Test := false)
    .configs(TemplateTest)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .settings(testOptions in TemplateTest := Seq(Tests.Filter(templateSpecFilter)))
    .settings(javaOptions in Test += configPath)
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)

  val common = play.Project(
    appName + "-common", Version.thisApp, appDependencies, file("modules/common"),
    settings = Common.baseSettings ++ SassPlugin.sassSettings
  ).settings(Keys.fork in Test := false)
    .dependsOn(govukTemplate % allPhases)
    .configs(TemplateTest)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .settings(testOptions in TemplateTest := Seq(Tests.Filter(templateSpecFilter)))
    .settings(javaOptions in Test += configPath)
    .settings(jasmineTestDir <+= baseDirectory { src => src / "test" / "views" / "common" })
    .settings(
      // Where does the UI live?
      uiDirectory <<= (baseDirectory in Compile) { _ / "ui" }
    )

  val paye = play.Project(
    appName + "-paye", Version.thisApp, appDependencies, path = file("modules/paye"), settings = Common.baseSettings ++ Common.routesImports
  ).settings(Keys.fork in Test := false)
    .dependsOn(common % allPhases)
    .configs(TemplateTest)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .settings(testOptions in TemplateTest := Seq(Tests.Filter(templateSpecFilter)))
    .settings(javaOptions in Test += configPath)
    .settings(jasmineTestDir <+= baseDirectory { src => src / "test" / "views" / "paye" })

  val preferences = play.Project(
    appName + "-preferences", Version.thisApp,
    appDependencies ++ Seq(Dependencies.Compile.scalaUri, Dependencies.Compile.scct),
    path = file("modules/preferences"), settings = Common.baseSettings ++ Common.routesImports
  ).settings(Keys.fork in Test := false)
    .dependsOn(common % allPhases)
    .configs(TemplateTest)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .settings(testOptions in TemplateTest := Seq(Tests.Filter(templateSpecFilter)))
    .settings(javaOptions in Test += configPath)

  val bt = play.Project(
  appName + "-business-tax", Version.thisApp, appDependencies, path = file("modules/business-tax"), settings = Common.baseSettings ++ Common.routesImports
  ).settings(Keys.fork in Test := false)
    .dependsOn(common % allPhases)
    .configs(TemplateTest)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .settings(testOptions in TemplateTest := Seq(Tests.Filter(templateSpecFilter)))
    .settings(javaOptions in Test += configPath)

  val sa = play.Project(
  appName + "-sa", Version.thisApp, appDependencies, path = file("modules/sa"), settings = Common.baseSettings ++ Common.routesImports
  ).settings(Keys.fork in Test := false)
    .dependsOn(common % allPhases, bt)
    .configs(TemplateTest)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .settings(testOptions in TemplateTest := Seq(Tests.Filter(templateSpecFilter)))
    .settings(javaOptions in Test += configPath)


  lazy val govukTax = play.Project(
    appName,
    Version.thisApp, appDependencies,
    settings = Common.baseSettings ++ SassPlugin.sassSettings
  ).settings(publishArtifact := true,
    Keys.fork in Test := false)
    .dependsOn(paye, sa, bt, preferences)
    .aggregate(common, paye, sa, bt, preferences, govukTemplate)

}

object Common {
  private val scalaSettings =
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
      testOptions in Test += Tests.Argument("-u", "target/test-reports", "-h", "target/test-reports/html-report"),
      testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD")
    )

  private val jasmineTestSettings =
    jasmineSettings ++ Seq(
      appJsDir <+= baseDirectory { _ / ".." / "common" / "public" / "javascripts"},
      appJsLibDir <+= baseDirectory { _ / ".." / "common" / "public" / "javascripts" / "vendor"},
      jasmineConfFile <+= baseDirectory { _ / ".." / "common" / "public" / "javascripts" / "test" /  "test.dependencies.js"},
      (test in Test) <<= (test in Test) dependsOn jasmine
    )

  val baseSettings =
    Defaults.defaultSettings ++ scalaSettings ++ jasmineTestSettings ++ playScalaSettings ++ Repositories.publishingSettings

  val routesImports = routesImport ++= Seq("uk.gov.hmrc.common.QueryBinders._", "uk.gov.hmrc.domain._")
}
