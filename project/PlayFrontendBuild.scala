import sbt._
import Keys._
import play.Project._
import net.litola.SassPlugin
import scala._
import com.gu.SbtJasminePlugin._
import scala.util.Properties._
import uk.gov.hmrc.{OptionalDependencies, MicroserviceDependencies}

object PlayFrontendBuild extends Build {

  val appName = "sa-prefs"
  val thisApp = envOrElse("SA_PREFS_VERSION", "999-SNAPSHOT")

  val allPhases = "tt->test;test->test;test->compile;compile->compile"

  def templateSpecFilter(name: String): Boolean = name endsWith "TemplateSpec"

  lazy val TemplateTest = config("tt") extend Test

  val uiDirectory = SettingKey[File]("ui-directory")


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
    Dependencies.Compile.scalaUri,
    Dependencies.Compile.metricsGraphite,

    MicroserviceDependencies.Test.junit,
    MicroserviceDependencies.Test.scalaTest,
    MicroserviceDependencies.Test.mockito,
    Dependencies.Test.jsoup,
    Dependencies.Test.pegdown
  )

  val providedByContainer = Seq(
      "uk.gov.hmrc" %% "govuk-template" % envOrElse("GOVUK_TEMPLATE_FRONTEND_VERSION", "999-SNAPSHOT"),
      "uk.gov.hmrc" %% "play-frontend" % envOrElse("PLAY_FRONTEND_VERSION", "999-SNAPSHOT"),

      "uk.gov.hmrc" %% "play-frontend" % envOrElse("PLAY_FRONTEND_VERSION", "999-SNAPSHOT") % "test" classifier "tests"
      )

  val paye = play.Project(appName, thisApp, appDependencies ++ providedByContainer, path = file("."), 
    settings = Common.baseSettings ++ Common.routesImports
  ).settings(Keys.fork in Test := false)
    .configs(TemplateTest)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .settings(testOptions in TemplateTest := Seq(Tests.Filter(templateSpecFilter)))

}

object Common {
  private val scalaSettings =
    Seq(
      organization := "uk.gov.hmrc",
      version := PlayFrontendBuild.thisApp,
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

  val baseSettings = Defaults.defaultSettings ++ scalaSettings ++ playScalaSettings ++ Repositories.publishingSettings

  val routesImports = routesImport ++= Seq("uk.gov.hmrc.common.QueryBinders._", "uk.gov.hmrc.domain._")
}

