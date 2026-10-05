resolvers += Classpaths.sbtPluginReleases

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

addDependencyTreePlugin

addSbtPlugin("org.scalameta"  % "sbt-scalafmt"             % "2.5.5")
addSbtPlugin("org.scoverage"  % "sbt-scoverage"            % "2.3.1")
addSbtPlugin("com.typesafe"   % "sbt-mima-plugin"          % "1.1.4")
addSbtPlugin("com.etsy"       % "sbt-compile-quick-plugin" % "1.4.0")
addSbtPlugin("dev.zio"        % "zio-sbt-website"          % "0.8.5")
addSbtPlugin("com.github.sbt" % "sbt-ci-release"           % "1.12.1")
addSbtPlugin("dev.zio"        % "zio-sbt-ci"               % "0.8.5")
