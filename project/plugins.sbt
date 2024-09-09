resolvers += Classpaths.sbtPluginReleases

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

addDependencyTreePlugin

addSbtPlugin("org.scalameta"  % "sbt-scalafmt"             % "2.5.2")
addSbtPlugin("org.scoverage"  % "sbt-scoverage"            % "2.1.1")
addSbtPlugin("com.typesafe"   % "sbt-mima-plugin"          % "1.1.3")
addSbtPlugin("com.etsy"       % "sbt-compile-quick-plugin" % "1.4.0")
addSbtPlugin("dev.zio"        % "zio-sbt-website"          % "0.3.10")
addSbtPlugin("com.github.sbt" % "sbt-ci-release"           % "1.5.12")
