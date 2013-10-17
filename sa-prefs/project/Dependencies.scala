import sbt._
import sbt.Keys._
import scala.Some

object Version {
  val thisApp = "0.0.1-SNAPSHOT"
  val scala = "2.10.3"
}

object Dependencies {

  object Compile {
    val nscalaTime = "com.github.nscala-time" %% "nscala-time" % "0.6.0"
    val json4sJackson = "org.json4s" %% "json4s-jackson" % "3.2.4"
    val json4sExt = "org.json4s" %% "json4s-ext" % "3.2.4"
    val commonsLang = "commons-lang" % "commons-lang" % "2.6"
    val commonsIo = "commons-io" % "commons-io" % "2.4"
    val guava = "com.google.guava" % "guava" % "14.0.1"
    val playMetrics = "com.kenshoo" %% "metrics-play" % "0.1.1"
    val metricsGraphite = "com.codahale.metrics" % "metrics-graphite" % "3.0.0"
    val secureUtils = "uk.gov.hmrc" % "secure-utils" % "0.1.0-SNAPSHOT"
  }

  sealed abstract class Test(scope: String) {

    val junit = "junit" % "junit" % "4.11" % "test"
    val scalaTest = "org.scalatest" %% "scalatest" % "2.0.RC2" % "test"
    val mockito = "org.mockito" % "mockito-all" % "1.9.5" % "test"
    val jsoup = "org.jsoup" % "jsoup" % "1.7.2"% "test"
  }

  object Test extends Test("test")

  object IntegrationTest extends Test("it")
}

object Repositories {

  import sbt.Keys._

  val hmrcNexusReleases = "hmrc-releases" at "https://nexus-preview.tax.service.gov.uk/content/repositories/hmrc-releases"
  val hmrcNexusSnapshots = "hmrc-snapshots" at "https://nexus-preview.tax.service.gov.uk/content/repositories/hmrc-snapshots"
  val hmrcThirdpartyReleases = "thirdparty-releases" at "https://nexus-preview.tax.service.gov.uk/content/repositories/thirdparty-releases"
  val hmrcThirdpartySnapshots = "thirdparty-snapshots" at "https://nexus-preview.tax.service.gov.uk/content/repositories/thirdparty-snapshots"

  val resolvers = Seq(
    Resolver.mavenLocal,
    hmrcNexusReleases,
    hmrcNexusSnapshots,
    hmrcThirdpartyReleases,
    hmrcThirdpartySnapshots,
    Opts.resolver.sonatypeReleases,
    Opts.resolver.sonatypeSnapshots
  )

  val publishingSettings = Seq(
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
    publishArtifact := true,
    publishArtifact in Test := true,
    publishTo <<= version {
      (v: String) =>
        if (v.trim.endsWith("SNAPSHOT"))
          Some(hmrcNexusSnapshots)
        else
          Some(hmrcNexusReleases)
    }
  )
}
 
