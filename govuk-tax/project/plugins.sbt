resolvers ++= Seq("Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
				"Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases",
				"Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/")


addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.0")

addSbtPlugin("net.litola" % "play-sass" % "0.3.0")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.2")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.2")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8")

addSbtPlugin("com.eed3si9n" % "sbt-dirty-money" % "0.1.0")