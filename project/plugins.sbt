addSbtPlugin("net.litola" % "play-sass" % "0.3.0")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

val hmrcRepoHost = java.lang.System.getProperty("hmrc.repo.host", "https://nexus-preview.tax.service.gov.uk")

resolvers ++= Seq("hmrc-snapshots" at hmrcRepoHost + "/content/repositories/hmrc-snapshots",
  "hmrc-releases" at hmrcRepoHost + "/content/repositories/hmrc-releases",
  "typesafe-releases" at hmrcRepoHost + "/content/repositories/typesafe-releases")

addSbtPlugin("uk.gov.hmrc" % "sbt-microservice" % "2.4.0")
