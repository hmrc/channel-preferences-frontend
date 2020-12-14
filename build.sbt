import Dependencies.oneForkedJvmPerTest
import play.sbt.routes.RoutesKeys.routesImport
import play.sbt.PlayImport._
import sbt.{Def, _}
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, integrationTestSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.ExternalService
import uk.gov.hmrc.ServiceManagerPlugin.Keys.itDependenciesList
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

  val appName = "preferences-frontend"

lazy val externalServices = List(
  ExternalService("AUTH"),
  ExternalService("AUTH_LOGIN_API"),
  ExternalService("USER_DETAILS"),
  ExternalService(name = "PREFERENCES", enableTestOnlyEndpoints = true, extraConfig = Map("featureFlag.switchOn" -> "true")),
  ExternalService("DATASTREAM"),
  ExternalService("ENTITY_RESOLVER"),
  ExternalService("MAILGUN_STUB"),
  ExternalService("HMRC_EMAIL_RENDERER"),
  ExternalService("IDENTITY_VERIFICATION", enableTestOnlyEndpoints = true),
  ExternalService("EMAIL")
)

lazy val TemplateTest = config("tt") extend Test
lazy val FunctionalTest = config("functional") extend Test
lazy val TemplateItTest = config("tit") extend IntegrationTest



   lazy val appDependencies = Dependencies.requiredDependencies

   lazy val playSettings = Seq(
    routesImport ++= Seq(
      "uk.gov.hmrc.domain._",
      "controllers.Assets"
    ))



lazy val plugins: Seq[Plugins] = Seq(play.sbt.PlayScala, SbtDistributablesPlugin)



lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(majorVersion := 8)
  .enablePlugins(plugins: _*)
  .settings(playSettings: _*)
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
  .settings(scalaVersion := "2.11.12")
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.govukfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._"
    )
  )
  .settings(
    targetJvm := "jvm-1.8",
    libraryDependencies ++= appDependencies,
    parallelExecution in Test := false,
    fork in Test := false,
    retrieveManaged := true
  )
  .settings(inConfig(FunctionalTest)(Defaults.testSettings): _*)
  .configs(FunctionalTest)
  .settings(
    Keys.fork in FunctionalTest := false,
    testOptions in FunctionalTest := List(
      Tests.Argument(
        "-o",
        "-u",
        s"${target.value.getPath}/functional-test-reports",
        "-h",
        s"${target.value.getPath}/functional-test-reports/html-report"
      ),
      Tests.Setup(() => sys.props += "browser" -> "chrome")
    ),
    unmanagedSourceDirectories in FunctionalTest := (baseDirectory in FunctionalTest)(base =>
      Seq(base / "functional")).value,
    parallelExecution in FunctionalTest := false
  )
  .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings())
  .settings(inConfig(TemplateItTest)(Defaults.itSettings): _*)
  .settings(ServiceManagerPlugin.serviceManagerSettings)
  .settings(itDependenciesList := externalServices)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    inConfig(IntegrationTest)(
      scalafmtCoreSettings ++
        Seq(
          compileInputs in compile := Def.taskDyn {
            val task = test in (resolvedScoped.value.scope in scalafmt.key)
            val previousInputs = (compileInputs in compile).value
            task.map(_ => previousInputs)
          }.value
        )
    ),
    parallelExecution in IntegrationTest := false
  )
  .settings(
    resolvers += Resolver.jcenterRepo,
    resolvers += Resolver.bintrayRepo("hmrc", "releases"),
    resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"
  )
