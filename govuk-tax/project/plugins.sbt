credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.1")

addSbtPlugin("net.litola" % "play-sass" % "0.3.0")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.2")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8")

addSbtPlugin("com.eed3si9n" % "sbt-dirty-money" % "0.1.0")

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

val hmrcRepoHost = java.lang.System.getProperty("hmrc.repo.host", "https://nexus-preview.tax.service.gov.uk")

resolvers ++= Seq("hmrc-snapshots" at hmrcRepoHost + "/content/repositories/hmrc-snapshots",
                  "hmrc-releases" at hmrcRepoHost + "/content/repositories/hmrc-releases",
                  "typesafe-releases" at hmrcRepoHost + "/content/repositories/typesafe-releases")

val gitStampPluginVersion = scala.util.Properties.envOrElse("GIT_STAMP_VERSION", "999-SNAPSHOT")

val dependencyPluginVersion = scala.util.Properties.envOrElse("TP_DEPENDENCY_PLUGIN_VERSION", "999-SNAPSHOT")

val jasminePluginVersion = scala.util.Properties.envOrElse("JASMINE_PLUGIN_VERSION", "999-SNAPSHOT")

addSbtPlugin("uk.gov.hmrc" % "sbt-git-stamp" % gitStampPluginVersion)

addSbtPlugin("com.gu" % "sbt-jasmine-plugin" % jasminePluginVersion)

addSbtPlugin("uk.gov.hmrc" % "sbt-tp-dependency-plugin" % dependencyPluginVersion)
