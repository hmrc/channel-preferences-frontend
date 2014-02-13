import sbt._
import scala.Some
import scala.Some
import scala.util.Properties._
import uk.gov.hmrc.GitStampPlugin._

object Version {
  val scala = "2.10.3"
}

object Dependencies {

  object Compile {
    val json4sExt = "org.json4s" %% "json4s-ext" % "3.2.4"
    val commonsLang = "commons-lang" % "commons-lang" % "2.6"
    val commonsIo = "commons-io" % "commons-io" % "2.4"
    val guava = "com.google.guava" % "guava" % "14.0.1"
    val playMetrics = "com.kenshoo" %% "metrics-play" % "0.1.3"
    val metricsGraphite = "com.codahale.metrics" % "metrics-graphite" % "3.0.1"
    val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.0" exclude("com.typesafe.sbt", "sbt-pgp") exclude("com.github.scct", "scct_2.10")
    val scct = "com.github.scct" %% "scct" % "0.2.1"
  }

  sealed abstract class Test(scope: String) {
    val jsoup = "org.jsoup" % "jsoup" % "1.7.2"% scope
    val pegdown = "org.pegdown" % "pegdown" % "1.1.0" % scope
  }

  object Test extends Test("test")

  object IntegrationTest extends Test("it")
}

object Repositories {

  import sbt.Keys._

  private val hmrcRepoHost = System.getProperty("hmrc.repo.host", "https://nexus-preview.tax.service.gov.uk")

  private val hmrcNexusReleases = "hmrc-releases" at hmrcRepoHost + "/content/repositories/hmrc-releases"
  private val hmrcNexusSnapshots = "hmrc-snapshots" at hmrcRepoHost + "/content/repositories/hmrc-snapshots"
  private val hmrcThirdpartyReleases = "thirdparty-releases" at hmrcRepoHost + "/content/repositories/thirdparty-releases"
  private val hmrcThirdpartySnapshots = "thirdparty-snapshots" at hmrcRepoHost + "/content/repositories/thirdparty-snapshots"

  val resolvers = Seq(
    Resolver.mavenLocal,
    hmrcNexusReleases,
    hmrcNexusSnapshots,
    hmrcThirdpartyReleases,
    hmrcThirdpartySnapshots,
    Opts.resolver.sonatypeReleases,
    Opts.resolver.sonatypeSnapshots
  )

  lazy val dist = com.typesafe.sbt.SbtNativePackager.NativePackagerKeys.dist

  val publishDist = TaskKey[sbt.File]("publish-dist", "publish the dist artifact")

  lazy val publishingSettings = sbtrelease.ReleasePlugin.releaseSettings ++ Seq(

    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),

    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := false,
    publishArtifact in (Compile, packageBin) := true,

    artifact in publishDist ~= {
      (art: Artifact) => art.copy(`type` = "zip", extension = "zip")
    },

    publishDist <<= (target, normalizedName, version) map { (targetDir, id, version) =>
      val packageName = "%s-%s" format(id, version)
      targetDir / "universal" / (packageName + ".zip")
    },

    publishTo <<= version {
      (v: String) =>
        if (v.trim.endsWith("SNAPSHOT"))
          Some(hmrcNexusSnapshots)
        else
          Some(hmrcNexusReleases)
    }

  ) ++ addArtifact(artifact in publishDist, publishDist) ++ gitStampSettings

}

