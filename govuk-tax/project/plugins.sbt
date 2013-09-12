resolvers ++= Seq("Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
				"Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases",
				"Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/")


addSbtPlugin("play" % "sbt-plugin" % "2.1.4")

addSbtPlugin("net.litola" % "play-sass" % "0.2.0")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.1")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.2")

