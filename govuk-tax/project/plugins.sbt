// The Typesafe repository
resolvers ++= Seq("Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
				"Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases",
				"Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/")

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.1.3")

// Sass compilation plugin
addSbtPlugin("net.litola" % "play-sass" % "0.2.0")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.0.1")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.3")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.2")

