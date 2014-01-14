credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.1")

addSbtPlugin("net.litola" % "play-sass" % "0.3.0")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.2")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.2")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8")

addSbtPlugin("uk.gov.hmrc" % "sbt-git-stamp" % "1.0.1")
