import sbt._
import sbt.Keys._
import scala._
import scala.util.Properties._
import play.Project._
import uk.gov.hmrc.PlayMicroServiceBuild

object FrontendBuild extends Build {

  val appName = "sa-prefs"
  val appVersion = envOrElse("SA_PREFS_VERSION", "999-SNAPSHOT")

  object appSpecificDependencies {
    val compile = Seq(
      "uk.gov.hmrc" %% "govuk-template" % envOrElse("GOVUK_TEMPLATE_VERSION", "999-SNAPSHOT"),
      "uk.gov.hmrc" %% "play-frontend" % envOrElse("PLAY_FRONTEND_VERSION", "999-SNAPSHOT"),
      "uk.gov.hmrc" %% "tax-core" % envOrElse("TAX_CORE_VERSION", "999-SNAPSHOT"),
      "com.netaporter" %% "scala-uri" % "0.4.0" exclude("com.typesafe.sbt", "sbt-pgp") exclude("com.github.scct", "scct_2.10"),
      "com.github.scct" %% "scct" % "0.2.1"
    )

    val test = Seq(
      "uk.gov.hmrc" %% "play-frontend" % envOrElse("PLAY_FRONTEND_VERSION", "999-SNAPSHOT") % "test" classifier "tests",
      "org.mockito" % "mockito-all" % "1.9.5" % "test",
      "org.jsoup" % "jsoup" % "1.7.2" % "test"
    )

    val all = compile ++ test
  }

  lazy val microservice =
    PlayMicroServiceBuild(appName, appVersion, appSpecificDependencies.all)
      .settings(routesImport ++= Seq("uk.gov.hmrc.common.QueryBinders._", "uk.gov.hmrc.domain._"))
}
