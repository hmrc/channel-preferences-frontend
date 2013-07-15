// The Typesafe repository
resolvers ++= Seq("Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
				"Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases",
				"Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/")

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.1.2")

// Sass compilation plugin
addSbtPlugin("net.litola" % "play-sass" % "0.2.0")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.4.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.0.1")

