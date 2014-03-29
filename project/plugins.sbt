addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.2")

addSbtPlugin("net.litola" % "play-sass" % "0.3.0")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.6.4")

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

val hmrcRepoHost = java.lang.System.getProperty("hmrc.repo.host", "https://nexus-preview.tax.service.gov.uk")

resolvers ++= Seq("hmrc-snapshots" at hmrcRepoHost + "/content/repositories/hmrc-snapshots",
  "hmrc-releases" at hmrcRepoHost + "/content/repositories/hmrc-releases",
  "typesafe-releases" at hmrcRepoHost + "/content/repositories/typesafe-releases")

val sbtMicroservicePluginVersion = scala.util.Properties.envOrElse("SBT_MICROSERVICE_VERSION", "999-SNAPSHOT")

addSbtPlugin("uk.gov.hmrc" % "sbt-microservice" % sbtMicroservicePluginVersion)
