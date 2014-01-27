credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.1")

addSbtPlugin("net.litola" % "play-sass" % "0.3.0")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.2")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.2")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8")

addSbtPlugin("com.eed3si9n" % "sbt-dirty-money" % "0.1.0")

val hmrcRepoHost = java.lang.System.getProperty("hmrc.repo.host", "https://nexus-preview.tax.service.gov.uk")

resolvers ++= Seq("hmrc-snapshots" at hmrcRepoHost + "/content/repositories/hmrc-snapshots",
                  "hmrc-releases" at hmrcRepoHost + "/content/repositories/hmrc-releases",
                  "typesafe-releases" at hmrcRepoHost + "/content/repositories/typesafe-releases")

addSbtPlugin("uk.gov.hmrc" % "sbt-git-stamp" % "1.0.1")

addSbtPlugin("com.gu" % "sbt-jasmine-plugin" % "0.8")

addSbtPlugin("uk.gov.hmrc" % "sbt-tp-dependency-plugin" % "2.0.1")
