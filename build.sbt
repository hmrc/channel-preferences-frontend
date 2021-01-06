import uk.gov.hmrc.DefaultBuildSettings.{ defaultSettings, integrationTestSettings, scalaSettings }
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import scoverage.ScoverageKeys
import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import uk.gov.hmrc.ExternalService
import uk.gov.hmrc.SbtBobbyPlugin.BobbyKeys.bobbyRulesURL
import uk.gov.hmrc.ServiceManagerPlugin.Keys.itDependenciesList
import wartremover.Wart
import wartremover.WartRemover.autoImport.{ wartremoverErrors, wartremoverExcluded }
import play.twirl.sbt.Import.TwirlKeys

val appName = "channel-preferences-frontend"

val silencerVersion = "1.7.0"

lazy val externalServices = List(
  ExternalService("DATASTREAM")
)

lazy val wartremoverSettings =
  Seq(
    wartremoverErrors in (Compile, compile) ++= Warts.allBut(
      Wart.Equals,
      Wart.ImplicitParameter,
      Wart.Nothing,
      Wart.DefaultArguments,
      Wart.Option2Iterable
    ),
    wartremoverErrors in (Test, compile) --= Seq(Wart.NonUnitStatements),
    wartremoverExcluded in (Compile, compile) ++= routes.in(Compile).value,
    wartremoverExcluded += (target in TwirlKeys.compileTemplates).value
  )

lazy val scoverageSettings =
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*Reverse.*;.*(config|testonly|views).*;.*(BuildInfo|Routes).*",
    ScoverageKeys.coverageMinimum := 90.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SwaggerPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(scalaSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(
    majorVersion := 0,
    scalaVersion := "2.12.12",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.channelpreferencesfrontend.config.AppConfig",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.govukfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._"
    ),
    PlayKeys.playDefaultPort := 9053,
    retrieveManaged := true,
    evictionWarningOptions in update :=
      EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    resolvers ++= Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.jcenterRepo
    ),
    // ***************
    // Use the silencer plugin to suppress warnings
    scalacOptions ++= Seq(
      "-P:silencer:pathFilters=target/.*",
      s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}",
      "-P:wartremover:excluded:/conf/app.routes",
      "-P:silencer:pathFilters=app.routes",
      "-P:wartremover:traverser:org.wartremover.warts.Unsafe",
      "-deprecation", // Emit warning and location for usages of deprecated APIs.
      "-encoding",
      "utf-8", // Specify character encoding used by source files.
      "-explaintypes", // Explain type errors in more detail.
      "-feature", // Emit warning and location for usages of features that should be imported explicitly.
      "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
      "-language:experimental.macros", // Allow macro definition (besides implementation and application)
      "-language:higherKinds", // Allow higher-kinded types
      "-language:implicitConversions", // Allow definition of implicit functions called views
      "-unchecked", // Enable additional warnings where generated code depends on assumptions.
      "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
      "-Xfatal-warnings", // Fail the compilation if there are any warnings.
      "-Xfuture", // Turn on future language features.
      "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
      "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
      "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
      "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
      "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
      "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
      "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
      "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
      "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
      "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
      "-Xlint:option-implicit", // Option.apply used implicit view.
      "-Xlint:package-object-classes", // Class or object defined in package object.
      "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
      "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
      "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
      "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
      "-Xlint:unsound-match", // Pattern match may not be typesafe.
      "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
      "-Ypartial-unification", // Enable partial unification in type constructor inference
      "-Ywarn-dead-code", // Warn when dead code is identified.
      "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
      "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
      "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
      "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
      "-Ywarn-nullary-unit", // Warn when nullary methods return Unit.
      "-Ywarn-numeric-widen", // Warn when numerics are widened.
      "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
      "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
      "-Ywarn-unused:locals", // Warn if a local definition is unused.
      "-Ywarn-unused:params", // Warn if a value parameter is unused.
      "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
      "-Ywarn-unused:privates", // Warn if a private member is unused.
      "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
    ),
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )
    // ***************
  )
  .settings(
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*(config|views.*);.*(AuthService|BuildInfo|Routes).*",
    ScoverageKeys.coverageMinimum := 68,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
  .settings(
    resolvers += Resolver.jcenterRepo,
    inConfig(IntegrationTest)(
      scalafmtCoreSettings ++
        Seq(compileInputs in compile := Def.taskDyn {
          val task = test in (resolvedScoped.value.scope in scalafmt.key)
          val previousInputs = (compileInputs in compile).value
          task.map(_ => previousInputs)
        }.value)
    )
  )
  .settings(ServiceManagerPlugin.serviceManagerSettings)
  .settings(itDependenciesList := externalServices)
  .settings(wartremoverSettings: _*)
  .settings(scoverageSettings: _*)

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
compileScalastyle := scalastyle.in(Compile).toTask("").value
(compile in Compile) := ((compile in Compile) dependsOn compileScalastyle).value

bobbyRulesURL := Some(new URL("https://webstore.tax.service.gov.uk/bobby-config/deprecated-dependencies.json"))
scalafmtOnCompile := true

lazy val silencerSettings: Seq[Setting[_]] = {
  val silencerVersion = "1.7.0"
  Seq(
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full)
    )
  )
}

(compile in Compile) := ((compile in Compile) dependsOn dependencyUpdates).value
dependencyUpdatesFilter -= moduleFilter(name = "flexmark-all")
dependencyUpdatesFilter -= moduleFilter(organization = "org.scala-lang")
dependencyUpdatesFilter -= moduleFilter(organization = "com.github.ghik")
dependencyUpdatesFilter -= moduleFilter(organization = "com.typesafe.play")
dependencyUpdatesFilter -= moduleFilter(organization = "org.scalatest")
dependencyUpdatesFilter -= moduleFilter(organization = "org.scalatestplus.play")
dependencyUpdatesFailBuild := true
sources in (Compile, doc) := Seq.empty

swaggerTarget := baseDirectory.value / "public"
swaggerFileName := "schema.json"
swaggerPrettyJson := true
swaggerRoutesFile := "prod.routes"
swaggerV3 := true

val codeStyleIntegrationTest = taskKey[Unit]("enforce code style then integration test")

// and then in settings...
Project.inConfig(IntegrationTest)(ScalastylePlugin.rawScalastyleSettings()) ++
  Seq(
    scalastyleConfig in IntegrationTest := (scalastyleConfig in scalastyle).value,
    scalastyleTarget in IntegrationTest := target.value / "scalastyle-it-results.xml",
    scalastyleFailOnError in IntegrationTest := (scalastyleFailOnError in scalastyle).value,
    (scalastyleFailOnWarning in IntegrationTest) := (scalastyleFailOnWarning in scalastyle).value,
    scalastyleSources in IntegrationTest := (unmanagedSourceDirectories in IntegrationTest).value,
    codeStyleIntegrationTest := scalastyle.in(IntegrationTest).toTask("").value,
    (test in IntegrationTest) := ((test in IntegrationTest) dependsOn codeStyleIntegrationTest).value
  )
