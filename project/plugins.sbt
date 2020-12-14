resolvers += Resolver.url("HMRC Sbt Plugin Releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(
  Resolver.ivyStylePatterns)
resolvers += Resolver.url(
  "HMRC Private Sbt Plugin Releases",
  url("https://artefacts.tax.service.gov.uk/artifactory/hmrc-sbt-plugin-releases-local"))(Resolver.ivyStylePatterns)
resolvers += "HMRC Releases" at "https://dl.bintray.com/hmrc/releases"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.25")

addSbtPlugin("uk.gov.hmrc" % "sbt-settings" % "4.6.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "2.1.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "2.12.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-git-versioning" % "2.2.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-artifactory" % "1.11.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-service-manager" % "0.6.0")

addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.16")

addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "1.4.13")
