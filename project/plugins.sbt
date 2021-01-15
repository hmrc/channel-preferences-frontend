resolvers += Resolver.url(
  "HMRC Sbt Plugin Releases",
  url("https://dl.bintray.com/hmrc/sbt-plugin-releases")
)(Resolver.ivyStylePatterns)
resolvers += "HMRC Releases" at "https://dl.bintray.com/hmrc/releases"
resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"        % "2.12.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-git-versioning"    % "2.2.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables"    % "2.1.0")
addSbtPlugin("com.typesafe.play" % "sbt-plugin"            % "2.7.7")
addSbtPlugin("org.scalastyle"    % "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("com.lucidchart"    % "sbt-scalafmt"          % "1.16")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"         % "1.6.1")
addSbtPlugin("org.wartremover"   % "sbt-wartremover"       % "2.4.13")
addSbtPlugin("org.irundaia.sbt"  % "sbt-sassify"           % "1.4.11")
addSbtPlugin("uk.gov.hmrc"       % "sbt-settings"          % "4.5.0")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"           % "0.5.1")
addSbtPlugin("uk.gov.hmrc"       % "sbt-service-manager"   % "0.8.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-bobby"             % "3.2.0")
