import ReleaseTransformations._

lazy val quill = 
  (project in file("."))
    .settings(tutSettings ++ commonSettings ++ Seq(
      scalaVersion := "2.11.7", 
      tutSourceDirectory := baseDirectory.value / "target" / "README.md"))
    .settings(sourceGenerators in Compile <+= Def.task {
      val source = baseDirectory.value / "README.md"
      val file = baseDirectory.value / "target" / "README.md"
      val str = IO.read(source).replace("```scala", "```tut")
      IO.write(file, str)
      Seq()
    })
    .dependsOn(`quill-core`, `quill-sql`, `quill-jdbc`, `quill-finagle-mysql`, `quill-async`, `quill-cassandra`)
    .aggregate(`quill-core`, `quill-sql`, `quill-jdbc`, `quill-finagle-mysql`, `quill-async`, `quill-cassandra`)

lazy val `quill-core` = 
  (project in file("quill-core"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe"               %  "config"        % "1.3.0",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
      "org.scala-lang"             %  "scala-reflect" % scalaVersion.value
    ))

lazy val `quill-sql` = 
  (project in file("quill-sql"))
    .settings(commonSettings: _*)
    .dependsOn(`quill-core` % "compile->compile;test->test")

lazy val `quill-jdbc` = 
  (project in file("quill-jdbc"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "com.zaxxer"     % "HikariCP"             % "2.3.9",
        "mysql"          % "mysql-connector-java" % "5.1.36" % "test",
        "org.postgresql" % "postgresql"           % "9.4-1206-jdbc41"
      ),
      parallelExecution in Test := false
    )
    .dependsOn(`quill-sql` % "compile->compile;test->test")

lazy val `quill-finagle-mysql` = 
  (project in file("quill-finagle-mysql"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "com.twitter" %% "finagle-mysql" % "6.31.0"
      ),
      parallelExecution in Test := false
    )
    .dependsOn(`quill-sql` % "compile->compile;test->test")

lazy val `quill-async` = 
  (project in file("quill-async"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "com.github.mauricio" %% "db-async-common" % "0.2.18",
        "com.github.mauricio" %% "mysql-async" % "0.2.18",
        "com.github.mauricio" %% "postgresql-async" % "0.2.18"
      ),
      parallelExecution in Test := false
    )
    .dependsOn(`quill-sql` % "compile->compile;test->test")

lazy val `quill-cassandra` = 
  (project in file("quill-cassandra"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.9",
        "org.monifu" %% "monifu" % "1.0"
      ),
      parallelExecution in Test := false
    )
    .dependsOn(`quill-core` % "compile->compile;test->test")

lazy val commonSettings = Seq(
  organization := "io.getquill",
  scalaVersion := "2.11.7",
  libraryDependencies ++= Seq(
    "org.scalamacros" %% "resetallattrs"  % "1.0.0",
    "org.scalatest"   %% "scalatest"      % "2.2.4" % "test",
    "ch.qos.logback"  % "logback-classic" % "1.1.3" % "test"
  ),
  EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource,
  unmanagedClasspath in Test += baseDirectory.value / "src" / "test" / "resources",
  scalacOptions ++= Seq(
    "-deprecation",           
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-Xlint",
    "-Yno-adapted-args",       
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",   
    "-Ywarn-value-discard",
    "-Xfuture",
    "-Ywarn-unused-import"
  ),
  fork in Test := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  scoverage.ScoverageKeys.coverageMinimum := 100,
  scoverage.ScoverageKeys.coverageFailOnMinimum := false,
  EclipseKeys.eclipseOutput := Some("bin"),
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  pgpSecretRing := file("local.secring.gpg"),
  pgpPublicRing := file("local.pubring.gpg"),
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(action = Command.process("publishSigned", _)),
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  ),
  pomExtra := (
    <url>http://github.com/fwbrasil/quill</url>
    <licenses>
      <license>
        <name>Apache License 2.0</name>
        <url>https://raw.githubusercontent.com/getquill/quill/master/LICENSE.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:fwbrasil/quill.git</url>
      <connection>scm:git:git@github.com:fwbrasil/quill.git</connection>
    </scm>
    <developers>
      <developer>
        <id>fwbrasil</id>
        <name>Flavio W. Brasil</name>
        <url>http://github.com/fwbrasil/</url>
      </developer>
    </developers>)
)

