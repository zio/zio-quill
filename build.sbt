import ReleaseTransformations._
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._
import sbtrelease.ReleasePlugin
import scala.sys.process.Process
import sbtcrossproject.crossProject
import java.io.{File => JFile}

enablePlugins(TutPlugin)

lazy val baseModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-core-jvm`, `quill-core-js`, `quill-sql-jvm`, `quill-sql-js`, `quill-monix`
)

lazy val dbModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-jdbc`, `quill-jdbc-monix`
)

lazy val asyncModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-async`, `quill-async-mysql`, `quill-async-postgres`,
  `quill-finagle-mysql`, `quill-finagle-postgres`
)

lazy val codegenModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-codegen`, `quill-codegen-jdbc`, `quill-codegen-tests`
)

lazy val bigdataModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-cassandra`, `quill-cassandra-lagom`, `quill-cassandra-monix`, `quill-orientdb`, `quill-spark`
)

lazy val allModules =
  baseModules ++ dbModules ++ asyncModules ++ codegenModules ++ bigdataModules

lazy val filteredModules = {
  val modulesStr = sys.props.get("modules")
  println(s"Modules Argument Value: ${modulesStr}")

  modulesStr match {
    case Some("base") =>
      println("Compiling Base Modules")
      baseModules
    case Some("db") =>
      println("Compiling Database Modules")
      dbModules
    case Some("async") =>
      println("Compiling Async Database Modules")
      asyncModules
    case Some("codegen") =>
      println("Compiling Code Generator Modules")
      codegenModules
    case Some("bigdata") =>
      println("Compiling Big Data Modules")
      bigdataModules
    case _ =>
      println("Compiling All Modules")
      allModules
  }
}

lazy val `quill` =
  (project in file("."))
    .settings(commonSettings: _*)
    .settings(`tut-settings`:_*)
    .aggregate(filteredModules.map(_.project): _*)
    .dependsOn(filteredModules: _*)

publishArtifact in `quill` := false

lazy val superPure = new sbtcrossproject.CrossType {
  def projectDir(crossBase: File, projectType: String): File =
    projectType match {
      case "jvm" => crossBase
      case "js"  => crossBase / s".$projectType"
    }

  def sharedSrcDir(projectBase: File, conf: String): Option[File] =
    Some(projectBase.getParentFile / "src" / conf / "scala")

  override def projectDir(crossBase: File, projectType: sbtcrossproject.Platform): File =
    projectType match {
      case JVMPlatform => crossBase
      case JSPlatform  => crossBase / ".js"
    }
}

lazy val `quill-core` =
  crossProject(JVMPlatform, JSPlatform).crossType(superPure)
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe"               %  "config"        % "1.3.4",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "org.scala-lang"             %  "scala-reflect" % scalaVersion.value
    ))
    .jsSettings(
      libraryDependencies += "org.scala-js" %%% "scalajs-java-time" % "0.2.5",
      coverageExcludedPackages := ".*"
    )

lazy val `quill-core-jvm` = `quill-core`.jvm
lazy val `quill-core-js` = `quill-core`.js

lazy val `quill-sql` =
  crossProject(JVMPlatform, JSPlatform).crossType(superPure)
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .jsSettings(
      coverageExcludedPackages := ".*"
    )
    .dependsOn(`quill-core` % "compile->compile;test->test")

lazy val `quill-sql-jvm` = `quill-sql`.jvm
lazy val `quill-sql-js` = `quill-sql`.js


lazy val `quill-codegen` =
  (project in file("quill-codegen"))
    .settings(commonSettings: _*)
    .dependsOn(`quill-core-jvm` % "compile->compile;test->test")

lazy val `quill-codegen-jdbc` =
  (project in file("quill-codegen-jdbc"))
    .settings(commonSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "com.github.choppythelumberjack" %% "tryclose" % "1.0.0",
        "org.scala-lang" % "scala-compiler" % scalaVersion.value % Test
      )
    )
    .dependsOn(`quill-codegen` % "compile->compile;test->test")
    .dependsOn(`quill-jdbc` % "compile->compile;test->test")


val codegen = taskKey[Seq[File]]("Run Code Generation Phase for Integration Testing")

lazy val `quill-codegen-tests` =
  (project in file("quill-codegen-tests"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value % Test,
      fork in Test := true,
      (sourceGenerators in Test) += (codegen in Test),
      (excludeFilter in unmanagedSources) := excludePathsIfOracle {
        (unmanagedSourceDirectories in Test).value.map { dir =>
          (dir / "io" / "getquill" / "codegen" / "OracleCodegenTestCases.scala").getCanonicalPath
        } ++
        (unmanagedSourceDirectories in Test).value.map { dir =>
          (dir / "io" / "getquill" / "codegen" / "util" / "WithOracleContext.scala").getCanonicalPath
        }
      },
      (codegen in Test) := {
        def recrusiveList(file:JFile): List[JFile] = {
          if (file.isDirectory)
            Option(file.listFiles()).map(_.flatMap(child=> recrusiveList(child)).toList).toList.flatten
          else
            List(file)
        }
        val r = (runner in Compile).value
        val s = streams.value.log
        val sourcePath = sourceManaged.value
        val classPath = (fullClasspath in Test in `quill-codegen-jdbc`).value.map(_.data)

        // We could put the code generated products directly in the `sourcePath` directory but for some reason
        // intellij doesn't like it unless there's a `main` directory inside.
        val fileDir = new File(sourcePath, "main").getAbsoluteFile
        val dbs =
          Seq("testH2DB", "testMysqlDB", "testPostgresDB", "testSqliteDB", "testSqlServerDB"
          ) ++ includeIfOracle("testOracleDB")
        println(s"Running code generation for DBs: ${dbs.mkString(", ")}")
        r.run(
          "io.getquill.codegen.integration.CodegenTestCaseRunner",
          classPath,
          // If oracle tests are included, enable code generation for it
          fileDir.getAbsolutePath +: dbs,
          s
        )
        recrusiveList(fileDir)
      }
    )
    .dependsOn(`quill-codegen-jdbc` % "compile->test")

val includeOracle =
  sys.props.getOrElse("oracle", "false").toBoolean

val excludeTests =
  sys.props.getOrElse("excludeTests", "false").toBoolean

val skipPush =
  sys.props.getOrElse("skipPush", "false").toBoolean

val debugMacro =
  sys.props.getOrElse("debugMacro", "false").toBoolean

def includeIfOracle[T](t:T):Seq[T] =
  if (includeOracle) Seq(t) else Seq()

val skipTag =
  sys.props.getOrElse("skipTag", "false").toBoolean

lazy val `quill-jdbc` =
  (project in file("quill-jdbc"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(jdbcTestingSettings: _*)
    .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")

lazy val `quill-monix` =
  (project in file("quill-monix"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "io.monix"                %% "monix-eval"          % "3.0.0-RC3",
        "io.monix"                %% "monix-reactive"      % "3.0.0-RC3"
      )
    )
    .dependsOn(`quill-core-jvm` % "compile->compile;test->test")

lazy val `quill-jdbc-monix` =
  (project in file("quill-jdbc-monix"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(jdbcTestingSettings: _*)
    .settings(
      testGrouping in Test := {
        (definedTests in Test).value map { test =>
          if (test.name endsWith "IntegrationSpec")
            Tests.Group(name = test.name, tests = Seq(test), runPolicy = Tests.SubProcess(
              ForkOptions().withRunJVMOptions(Vector("-Xmx200m"))
            ))
          else
            Tests.Group(name = test.name, tests = Seq(test), runPolicy = Tests.SubProcess(ForkOptions()))
        }
      }
    )
    .dependsOn(`quill-monix` % "compile->compile;test->test")
    .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")
    .dependsOn(`quill-jdbc` % "compile->compile;test->test")

lazy val `quill-spark` =
  (project in file("quill-spark"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "org.apache.spark" %% "spark-sql" % "2.4.3"
      )
    )
    .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")

lazy val `quill-finagle-mysql` =
  (project in file("quill-finagle-mysql"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "com.twitter" %% "finagle-mysql" % "19.7.0"
      )
    )
    .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")

lazy val `quill-finagle-postgres` =
  (project in file("quill-finagle-postgres"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "io.github.finagle" %% "finagle-postgres" % "0.11.0"
      )
    )
    .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")

lazy val `quill-async` =
  (project in file("quill-async"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "com.github.mauricio" %% "db-async-common"  % "0.2.21"
      )
    )
    .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")

lazy val `quill-async-mysql` =
  (project in file("quill-async-mysql"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "com.github.mauricio" %% "mysql-async"      % "0.2.21"
      )
    )
    .dependsOn(`quill-async` % "compile->compile;test->test")

lazy val `quill-async-postgres` =
  (project in file("quill-async-postgres"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "com.github.mauricio" %% "postgresql-async" % "0.2.21"
      )
    )
    .dependsOn(`quill-async` % "compile->compile;test->test")

lazy val `quill-cassandra` =
  (project in file("quill-cassandra"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= Seq(
        "com.datastax.cassandra" %  "cassandra-driver-core" % "3.7.2"
      )
    )
    .dependsOn(`quill-core-jvm` % "compile->compile;test->test")

lazy val `quill-cassandra-monix` =
  (project in file("quill-cassandra-monix"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      fork in Test := true
    )
    .dependsOn(`quill-cassandra` % "compile->compile;test->test")
    .dependsOn(`quill-monix` % "compile->compile;test->test")

lazy val `quill-cassandra-lagom` =
   (project in file("quill-cassandra-lagom"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      fork in Test := true,
      libraryDependencies ++= {
        val lagomVersion = "1.5.1"
        Seq(
          "com.lightbend.lagom" %% "lagom-scaladsl-persistence-cassandra" % lagomVersion % Provided,
          "com.lightbend.lagom" %% "lagom-scaladsl-testkit" % lagomVersion % Test
        )
      }
    )
    .dependsOn(`quill-cassandra` % "compile->compile;test->test")


lazy val `quill-orientdb` =
  (project in file("quill-orientdb"))
      .settings(commonSettings: _*)
      .settings(mimaSettings: _*)
      .settings(
        fork in Test := true,
        libraryDependencies ++= Seq(
          "com.orientechnologies" % "orientdb-graphdb" % "3.0.22"
        )
      )
      .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")

lazy val `tut-sources` = Seq(
  "CASSANDRA.md",
  "README.md"
)

lazy val `tut-settings` = Seq(
  scalacOptions in Tut := Seq(),
  tutSourceDirectory := baseDirectory.value / "target" / "tut",
  tutNameFilter := `tut-sources`.map(_.replaceAll("""\.""", """\.""")).mkString("(", "|", ")").r,
  sourceGenerators in Compile +=
    Def.task {
      `tut-sources`.foreach { name =>
        val source = baseDirectory.value / name
        val file = baseDirectory.value / "target" / "tut" / name
        val str = IO.read(source).replace("```scala", "```tut")
        // workaround tut bug due to https://github.com/tpolecat/tut/pull/220
        val fixed = str.replaceAll("\\n//.*", "\n1").replaceAll("//.*", "")
        IO.write(file, fixed)
      }
      Seq()
    }.taskValue
)

lazy val mimaSettings = MimaPlugin.mimaDefaultSettings ++ Seq(
  mimaPreviousArtifacts := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, scalaMajor)) if scalaMajor <= 11 =>
        Set(organization.value % s"${name.value}_${scalaBinaryVersion.value}" % "0.5.0")
      case _ =>
        Set()
    }
  }
)

commands += Command.command("checkUnformattedFiles") { st =>
  val vcs = Project.extract(st).get(releaseVcs).get
  val modified = vcs.cmd("ls-files", "--modified", "--exclude-standard").!!.trim.split('\n').filter(_.contains(".scala"))
  if(modified.nonEmpty)
    throw new IllegalStateException(s"Please run `sbt scalariformFormat test:scalariformFormat` and resubmit your pull request. Found unformatted files: ${modified.toList}")
  st
}

def updateReadmeVersion(selectVersion: sbtrelease.Versions => String) =
  ReleaseStep(action = st => {

    val newVersion = selectVersion(st.get(ReleaseKeys.versions).get)

    import scala.io.Source
    import java.io.PrintWriter

    val pattern = """"io.getquill" %% "quill-.*" % "(.*)"""".r

    val fileName = "README.md"
    val content = Source.fromFile(fileName).getLines.mkString("\n")

    val newContent =
      pattern.replaceAllIn(content,
        m => m.matched.replaceAllLiterally(m.subgroups.head, newVersion))

    new PrintWriter(fileName) { write(newContent); close }

    val vcs = Project.extract(st).get(releaseVcs).get
    vcs.add(fileName).!

    st
  })

def updateWebsiteTag =
  ReleaseStep(action = st => {

    val vcs = Project.extract(st).get(releaseVcs).get
    vcs.tag("website", "update website", false).!

    st
  })

lazy val jdbcTestingSettings = Seq(
  fork in Test := true,
  resolvers ++= includeIfOracle( // read ojdbc8 jar in case it is deployed
    Resolver.mavenLocal
  ),
  libraryDependencies ++= {
    val deps =
      Seq(
        "com.zaxxer"              %  "HikariCP"                % "3.3.1",
        "mysql"                   %  "mysql-connector-java"    % "8.0.17"             % Test,
        "com.h2database"          %  "h2"                      % "1.4.199"            % Test,
        "org.postgresql"          %  "postgresql"              % "42.2.6"             % Test,
        "org.xerial"              %  "sqlite-jdbc"             % "3.28.0"           % Test,
        "com.microsoft.sqlserver" %  "mssql-jdbc"              % "7.1.1.jre8-preview" % Test,
        "org.mockito"             %% "mockito-scala-scalatest" % "1.5.12"              % Test
      )

    deps ++ includeIfOracle(
      "com.oracle.jdbc" % "ojdbc8" % "18.3.0.0.0" % Test
    )
  },
  excludeFilter in unmanagedSources := {
    excludeTests match {
      case true =>
        excludePaths((unmanagedSourceDirectories in Test).value.map(dir => dir.getCanonicalPath))
      case false =>
        excludePathsIfOracle {
          (unmanagedSourceDirectories in Test).value.map { dir =>
            (dir / "io" / "getquill" / "context" / "jdbc" / "oracle").getCanonicalPath
          } ++
            (unmanagedSourceDirectories in Test).value.map { dir =>
              (dir / "io" / "getquill" / "oracle").getCanonicalPath
            }
        }
    }
  }
)

def excludePaths(paths:Seq[String]) = {
  val excludeThisPath =
    (path: String) =>
      paths.exists { srcDir =>
        (path contains srcDir)
      }
  new SimpleFileFilter(file => {
    if (excludeThisPath(file.getCanonicalPath))
      println(s"Excluding: ${file.getCanonicalPath}")
    excludeThisPath(file.getCanonicalPath)
  })
}

def excludePathsIfOracle(paths:Seq[String]) = {
  val excludeThisPath =
    (path: String) =>
      paths.exists { srcDir =>
        !includeOracle && (path contains srcDir)
      }
  new SimpleFileFilter(file => {
    if (excludeThisPath(file.getCanonicalPath))
      println(s"Excluding Oracle Related File: ${file.getCanonicalPath}")
    excludeThisPath(file.getCanonicalPath)
  })
}

lazy val basicSettings = Seq(
  excludeFilter in unmanagedSources := {
    excludeTests match {
      case true  => excludePaths((unmanagedSourceDirectories in Test).value.map(dir => dir.getCanonicalPath))
      case false => new SimpleFileFilter(file => false)
    }
  },
  organization := "io.getquill",
  scalaVersion := "2.11.12",
  crossScalaVersions := Seq("2.11.12","2.12.7"),
  libraryDependencies ++= Seq(
    "org.scalamacros" %% "resetallattrs"  % "1.0.0",
    "org.scalatest"   %%% "scalatest"     % "3.0.8"          % Test,
    "ch.qos.logback"  % "logback-classic" % "1.2.3"          % Test,
    "com.github.choppythelumberjack" %% "tryclose" % "1.0.0" % Test,
    "com.google.code.findbugs" % "jsr305" % "3.0.2"          % Provided // just to avoid warnings during compilation
  ) ++ {
    if (debugMacro) Seq(
      "org.scala-lang"   %  "scala-library"     % scalaVersion.value,
      "org.scala-lang"   %  "scala-compiler"    % scalaVersion.value,
      "org.scala-lang"   %  "scala-reflect"     % scalaVersion.value
    )
    else Seq()
  },
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(AlignParameters, true)
    .setPreference(CompactStringConcatenation, false)
    .setPreference(IndentPackageBlocks, true)
    .setPreference(FormatXml, true)
    .setPreference(PreserveSpaceBeforeArguments, false)
    .setPreference(DoubleIndentConstructorArguments, false)
    .setPreference(RewriteArrowSymbols, false)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 40)
    .setPreference(SpaceBeforeColon, false)
    .setPreference(SpaceInsideBrackets, false)
    .setPreference(SpaceInsideParentheses, false)
    .setPreference(DanglingCloseParenthesis, Force)
    .setPreference(IndentSpaces, 2)
    .setPreference(IndentLocalDefs, false)
    .setPreference(SpacesWithinPatternBinders, true)
    .setPreference(SpacesAroundMultiImports, true),
  EclipseKeys.createSrc := EclipseCreateSrc.Default,
  unmanagedClasspath in Test ++= Seq(
    baseDirectory.value / "src" / "test" / "resources"
  ),
  EclipseKeys.eclipseOutput := Some("bin"),
  scalacOptions ++= Seq(
    "-Xfatal-warnings",
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture"
  ),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) =>
        Seq("-Xlint", "-Ywarn-unused-import", "" +
          "-Xsource:2.12" // needed so existential types work correctly
        )
      case Some((2, 12)) =>
        Seq("-Xlint:-unused,_",
          "-Ywarn-unused:imports",
          "-Ycache-macro-class-loader:last-modified"
        )
      case _ => Seq()
    }
  },
  concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
  scoverage.ScoverageKeys.coverageMinimum := 96,
  scoverage.ScoverageKeys.coverageFailOnMinimum := false
)

def doOnDefault(steps: ReleaseStep*): Seq[ReleaseStep] =
  Seq[ReleaseStep](steps: _*)

def doOnPush(steps: ReleaseStep*): Seq[ReleaseStep] =
  if (skipPush)
    Seq[ReleaseStep]()
  else
    Seq[ReleaseStep](steps: _*)

lazy val commonSettings = ReleasePlugin.extraReleaseCommands ++ basicSettings ++ Seq(
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
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
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseProcess := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) =>
        doOnDefault(checkSnapshotDependencies) ++
        doOnDefault(inquireVersions) ++
        doOnDefault(runClean) ++
        doOnDefault(setReleaseVersion) ++
        doOnDefault(updateReadmeVersion(_._1)) ++
        doOnPush   (commitReleaseVersion) ++
        doOnPush   (updateWebsiteTag) ++
        doOnPush   (tagRelease) ++
        doOnDefault(publishArtifacts) ++
        doOnPush   (setNextVersion) ++
        doOnPush   (updateReadmeVersion(_._2)) ++
        doOnPush   (commitNextVersion) ++
        //doOnPush(releaseStepCommand("sonatypeReleaseAll")) ++
        doOnPush   (pushChanges)
      case Some((2, 12)) =>
        doOnDefault(checkSnapshotDependencies) ++
        doOnDefault(inquireVersions) ++
        doOnDefault(runClean) ++
        doOnDefault(setReleaseVersion) ++
        doOnDefault(publishArtifacts)
        //doOnPush   ("sonatypeReleaseAll") ++
      case _ => Seq[ReleaseStep]()
    }
  },
  pomExtra := (
    <url>http://github.com/getquill/quill</url>
    <licenses>
      <license>
        <name>Apache License 2.0</name>
        <url>https://raw.githubusercontent.com/getquill/quill/master/LICENSE.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:getquill/quill.git</url>
      <connection>scm:git:git@github.com:getquill/quill.git</connection>
    </scm>
    <developers>
      <developer>
        <id>fwbrasil</id>
        <name>Flavio W. Brasil</name>
        <url>http://github.com/fwbrasil/</url>
      </developer>
    </developers>)
)
