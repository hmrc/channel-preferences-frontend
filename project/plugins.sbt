addSbtPlugin("net.litola" % "play-sass" % "0.3.0")

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

val hmrcRepoHost = java.lang.System.getProperty("hmrc.repo.host", "https://nexus-preview.tax.service.gov.uk")

resolvers ++= Seq("hmrc-snapshots" at hmrcRepoHost + "/content/repositories/hmrc-snapshots",
  "hmrc-releases" at hmrcRepoHost + "/content/repositories/hmrc-releases",
  "typesafe-releases" at hmrcRepoHost + "/content/repositories/typesafe-releases")

addSbtPlugin("uk.gov.hmrc" % "sbt-microservice" % "3.1.0")

addSbtPlugin("uk.gov.hmrc" % "hmrc-resolvers" % "0.1.0")

