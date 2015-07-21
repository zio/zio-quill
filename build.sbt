
lazy val root = 
  (project in file("."))
    .aggregate(`quill-core`, `quill-sql`, `quill-jdbc`)

lazy val `quill-core` = 
  (project in file("quill-core"))
    .settings(commonSettings: _*)

lazy val `quill-sql` = 
  (project in file("quill-sql"))
    .settings(commonSettings: _*)
    .dependsOn(`quill-core`)

lazy val `quill-jdbc` = 
  (project in file("quill-jdbc"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies += "com.zaxxer" % "HikariCP" % "2.3.9")
    .dependsOn(`quill-sql`)

lazy val commonSettings = releaseSettings ++ Seq(
  organization := "io.getquill",
  scalaVersion := "2.11.5",
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value
  ),
  scalacOptions ++= Seq("-deprecation", "-feature", "-Xfatal-warnings"),
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := (
    <url>http://github.com/fwbrasil/quill</url>
    <licenses>
      <license>
        <name>LGPL</name>
        <url>https://raw.githubusercontent.com/fwbrasil/quill/master/LICENSE-LGPL.txt</url>
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

