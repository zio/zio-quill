import ReleaseTransformations._
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._
import sbtrelease.ReleasePlugin
import sbtcrossproject.CrossPlugin.autoImport.crossProject
import java.io.{File => JFile}
import ReleaseTransformations._
import sbtrelease.Version
import sbtrelease.versionFormatError

// During release cycles, GPG will expect passphrase user-input EVEN when --passphrase is specified
// this should add --pinentry-loopback in order to disable that. See here for more info:
// https://github.com/sbt/sbt-pgp/issues/178
Global / useGpgPinentry := true

// Do not strip the qualifier, want to keep that. If I set version.sbt to 1.2.3.foo.1 that's exactly what I want the version to be
releaseVersion     := { ver => ver }
releaseNextVersion := { ver =>
  val withoutLast = ver.reverse.dropWhile(_.isDigit).reverse
  val last = ver.reverse.takeWhile(_.isDigit).reverse
  println(s"Detected original version: ${ver}. Which is ${withoutLast} + ${last}")
  // see if the last group of chars are numeric, if they are, just increment
  val actualLast = scala.util.Try(last.toInt).map(i => (i + 1).toString).getOrElse(last)
  val newVer = withoutLast + actualLast + "-SNAPSHOT"
  println(s"Final computed version is: ${newVer}")
  newVer
}

val CodegenTag = Tags.Tag("CodegenTag")
(Global / concurrentRestrictions) += Tags.exclusive(CodegenTag)
(Global / concurrentRestrictions) += Tags.limit(ScalaJSTags.Link, 1)

lazy val jsModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-core-portable-js`, `quill-core-js`,
  `quill-sql-portable-js`, `quill-sql-js`
)

lazy val baseModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-core-portable-jvm`,
  `quill-core-jvm`,
  `quill-sql-portable-jvm`,
  `quill-sql-jvm`, `quill-monix`, `quill-zio`
)

lazy val dbModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-jdbc`, `quill-jdbc-monix`, `quill-jdbc-zio`
)

lazy val jasyncModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-jasync`, `quill-jasync-postgres`, `quill-jasync-mysql`
)

lazy val asyncModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-async`, `quill-async-mysql`, `quill-async-postgres`,
  `quill-finagle-mysql`, `quill-finagle-postgres`,
  `quill-ndbc`, `quill-ndbc-postgres`, `quill-ndbc-monix`
) ++ jasyncModules

lazy val codegenModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-codegen`, `quill-codegen-jdbc`, `quill-codegen-tests`
)

lazy val bigdataModules = Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-cassandra`, `quill-cassandra-lagom`, `quill-cassandra-monix`, `quill-cassandra-zio`, `quill-orientdb`, `quill-spark`
)

lazy val allModules =
  baseModules ++ jsModules ++ dbModules ++ asyncModules ++ codegenModules ++ bigdataModules

lazy val scala213Modules = baseModules ++ jsModules ++ dbModules ++ codegenModules ++ Seq[sbt.ClasspathDep[sbt.ProjectReference]](
  `quill-async`,
  `quill-async-mysql`,
  `quill-async-postgres`,
  `quill-finagle-mysql`,
  `quill-cassandra`,
  `quill-cassandra-lagom`,
  `quill-cassandra-monix`,
  `quill-cassandra-zio`,
  `quill-orientdb`,
  `quill-jasync`,
  `quill-jasync-postgres`,
  `quill-jasync-mysql`
)

def isScala213 = {
  val scalaVersion = sys.props.get("quill.scala.version")
  scalaVersion.map(_.startsWith("2.13")).getOrElse(false)
}

val filteredModules = {
  val modulesStr = sys.props.get("modules")
  println(s"SBT =:> Modules Argument Value: ${modulesStr}")

  val modules = modulesStr match {
    case Some("base") =>
      println("SBT =:> Compiling Base Modules")
      baseModules
    case Some("js") =>
      println("SBT =:> Compiling JavaScript Modules")
      jsModules
    case Some("db") =>
      println("SBT =:> Compiling Database Modules")
      dbModules
    case Some("async") =>
      println("SBT =:> Compiling Async Database Modules")
      asyncModules
    case Some("codegen") =>
      println("SBT =:> Compiling Code Generator Modules")
      codegenModules
    case Some("nocodegen") =>
      println("Compiling Not-Code Generator Modules")
      baseModules ++ jsModules ++ dbModules ++ asyncModules ++ bigdataModules
    case Some("bigdata") =>
      println("SBT =:> Compiling Big Data Modules")
      bigdataModules
    case Some("none") =>
      println("SBT =:> Invoking Aggregate Project")
      Seq[sbt.ClasspathDep[sbt.ProjectReference]]()
    case _ =>
      // Workaround for https://github.com/sbt/sbt/issues/3465
      val scalaVersion = sys.props.get("quill.scala.version")
      if(scalaVersion.map(_.startsWith("2.13")).getOrElse(false)) {
        println("SBT =:> Compiling Scala 2.13 Modules")
        baseModules ++ dbModules ++ jasyncModules
      } else {
        println("SBT =:> Compiling All Modules")
        allModules
        // Note, can't do this because things like inform (i.e. formatting) actually do run for all modules
        //throw new IllegalStateException("Tried to build all modules. Not allowed.")
      }
  }
  if(isScala213) {
    println("SBT =:> Compiling 2.13 Modules Only")
    modules.filter(scala213Modules.contains(_))
  } else modules
}

lazy val `quill` =
  (project in file("."))
    .settings(commonSettings: _*)
    .aggregate(filteredModules.map(_.project): _*)
    .dependsOn(filteredModules: _*)

`quill` / publishArtifact := false

lazy val superPure = new sbtcrossproject.CrossType {
  def projectDir(crossBase: File, projectType: String): File =
    projectType match {
      case "jvm" => crossBase / s"$projectType"
      case "js"  => crossBase / s"$projectType"
    }

  def sharedSrcDir(projectBase: File, conf: String): Option[File] =
    Some(projectBase.getParentFile / "src" / conf / "scala")

  override def projectDir(crossBase: File, projectType: sbtcrossproject.Platform): File =
    projectType match {
      case JVMPlatform => crossBase / "jvm"
      case JSPlatform  => crossBase / "js"
    }
}

lazy val ultraPure = new sbtcrossproject.CrossType {
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

def pprintVersion(v: String) = {
  if(v.startsWith("2.11")) "0.5.4" else "0.5.9"
}

lazy val `quill-core-portable` =
  crossProject(JVMPlatform, JSPlatform).crossType(ultraPure)
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "com.typesafe"               %  "config"        % "1.4.1",
        "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
        "org.scala-lang"             %  "scala-reflect" % scalaVersion.value,
        "com.twitter"                %% "chill"         % "0.10.0",
        "io.suzaku"                  %% "boopickle"     % "1.3.1"
      ),
      coverageExcludedPackages := "<empty>;.*AstPrinter;.*Using;io.getquill.Model;io.getquill.ScalarTag;io.getquill.QuotationTag"
    )
    .jsSettings(
      libraryDependencies ++= Seq(
        "com.lihaoyi" %%% "pprint" % pprintVersion(scalaVersion.value),
        "org.scala-js" %%% "scalajs-java-time" % "0.2.5",
        "com.lihaoyi" %%% "pprint" % "0.5.4",
        "org.scala-js" %%% "scalajs-java-time" % "0.2.5",
        "io.suzaku" %%% "boopickle" % "1.3.1"
      ),
      coverageExcludedPackages := ".*",
      // 2.12 Build seems to take forever without this option
      Test / fastOptJS / scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) }
    ).enablePlugins(MimaPlugin)

lazy val `quill-core-portable-jvm` = `quill-core-portable`.jvm
lazy val `quill-core-portable-js` = `quill-core-portable`.js

lazy val `quill-core` =
  crossProject(JVMPlatform, JSPlatform).crossType(superPure)
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe"               %  "config"        % "1.4.1",
      "dev.zio"                    %% "zio-logging"   % "0.5.13",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
      "org.scala-lang"             %  "scala-reflect" % scalaVersion.value
    ))
    .jvmSettings(
      Test / fork := true
    )
    .jsSettings(
      libraryDependencies ++= Seq(
        "com.lihaoyi" %%% "pprint" % pprintVersion(scalaVersion.value),
        "org.scala-js" %%% "scalajs-java-time" % "0.2.5",
        "org.scala-js" %%% "scalajs-java-time" % "0.2.5"
      ),
      unmanagedSources / excludeFilter := new SimpleFileFilter(file => file.getName == "DynamicQuerySpec.scala"),
      coverageExcludedPackages := ".*",
      // 2.12 Build seems to take forever without this option
      Test / fastOptJS / scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) }
    )
    .dependsOn(`quill-core-portable` % "compile->compile")
    .enablePlugins(MimaPlugin)

// dependsOn in these clauses technically not needed however, intellij does not work properly without them
lazy val `quill-core-jvm` = `quill-core`.jvm.dependsOn(`quill-core-portable-jvm` % "compile->compile")
lazy val `quill-core-js` = `quill-core`.js.dependsOn(`quill-core-portable-js` % "compile->compile")

lazy val `quill-sql-portable` =
  crossProject(JVMPlatform, JSPlatform).crossType(ultraPure)
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.github.vertical-blank"  %% "scala-sql-formatter" % "1.0.0"
    ))
    .jsSettings(
      libraryDependencies ++= Seq(
        "com.github.vertical-blank" %%% "scala-sql-formatter" % "1.0.0"
      ),
      scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
      coverageExcludedPackages := ".*",
      // 2.12 Build seems to take forever without this option
      Test / fastOptJS / scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) }
      //jsEnv := NodeJSEnv(args = Seq("--max_old_space_size=1024")).value
    )
    .dependsOn(`quill-core-portable` % "compile->compile")
    .enablePlugins(MimaPlugin)

lazy val `quill-sql-portable-jvm` = `quill-sql-portable`.jvm
lazy val `quill-sql-portable-js` = `quill-sql-portable`.js


lazy val `quill-sql` =
  crossProject(JVMPlatform, JSPlatform).crossType(ultraPure)
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.github.vertical-blank"  %% "scala-sql-formatter" % "1.0.1"
    ))
    .jsSettings(
      libraryDependencies ++= Seq(
        "com.github.vertical-blank" %%% "scala-sql-formatter" % "1.0.1"
      ),
      scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
      coverageExcludedPackages := ".*",
      // 2.12 Build seems to take forever without this option
      Test / fastOptJS / scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) }
    )
    .dependsOn(
      `quill-sql-portable` % "compile->compile",
      `quill-core` % "compile->compile;test->test"
    )
    .enablePlugins(MimaPlugin)


lazy val `quill-sql-jvm` = `quill-sql`.jvm
lazy val `quill-sql-js` = `quill-sql`.js


lazy val `quill-codegen` =
  (project in file("quill-codegen"))
    .settings(commonSettings: _*)
    .dependsOn(`quill-core-jvm` % "compile->compile;test->test")

lazy val `quill-codegen-jdbc` =
  (project in file("quill-codegen-jdbc"))
    .settings(commonSettings: _*)
    .settings(jdbcTestingLibraries: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-compiler" % scalaVersion.value % Test
      )
    )
    .dependsOn(`quill-codegen` % "compile->compile;test->test")
    .dependsOn(`quill-jdbc` % "compile->compile")

lazy val `quill-codegen-tests` =
  (project in file("quill-codegen-tests"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value % Test,
      Test / fork := true,
      (Test / sourceGenerators) += Def.task {
        def recrusiveList(file:JFile): List[JFile] = {
          if (file.isDirectory)
            Option(file.listFiles()).map(_.flatMap(child=> recrusiveList(child)).toList).toList.flatten
          else
            List(file)
        }
        val r = (Compile / runner).value
        val s = streams.value.log
        val sourcePath = sourceManaged.value
        val classPath = (`quill-codegen-jdbc` / Test / fullClasspath).value.map(_.data)

        // We could put the code generated products directly in the `sourcePath` directory but for some reason
        // intellij doesn't like it unless there's a `main` directory inside.
        val fileDir = new File(sourcePath, "main").getAbsoluteFile
        val dbs = Seq("testH2DB", "testMysqlDB", "testPostgresDB", "testSqliteDB", "testSqlServerDB", "testOracleDB")
        println(s"Running code generation for DBs: ${dbs.mkString(", ")}")
        r.run(
          "io.getquill.codegen.integration.CodegenTestCaseRunner",
          classPath,
          fileDir.getAbsolutePath +: dbs,
          s
        )
        recrusiveList(fileDir)
      }.tag(CodegenTag)
    )
    .dependsOn(`quill-codegen-jdbc` % "compile->test")

val excludeTests =
  sys.props.getOrElse("excludeTests", "false").toBoolean

val skipPush =
  sys.props.getOrElse("skipPush", "false").toBoolean

val debugMacro =
  sys.props.getOrElse("debugMacro", "false").toBoolean

val skipTag =
  sys.props.getOrElse("skipTag", "false").toBoolean

lazy val `quill-jdbc` =
  (project in file("quill-jdbc"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(jdbcTestingSettings: _*)
    .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)

lazy val `quill-monix` =
  (project in file("quill-monix"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= Seq(
        "io.monix"                %% "monix-eval"          % "3.0.0",
        "io.monix"                %% "monix-reactive"      % "3.0.0"
      )
    )
    .dependsOn(`quill-core-jvm` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)

lazy val `quill-jdbc-monix` =
  (project in file("quill-jdbc-monix"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(jdbcTestingSettings: _*)
    .settings(
      Test / testGrouping := {
        (Test / definedTests).value map { test =>
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
    .enablePlugins(MimaPlugin)

lazy val `quill-zio` =
  (project in file("quill-zio"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio" % "1.0.12",
        "dev.zio" %% "zio-streams" % "1.0.12"
      )
    )
    .dependsOn(`quill-core-jvm` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)

lazy val `quill-jdbc-zio` =
  (project in file("quill-jdbc-zio"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(jdbcTestingSettings: _*)
    .settings(
      Test / testGrouping := {
        (Test / definedTests).value map { test =>
          if (test.name endsWith "IntegrationSpec")
            Tests.Group(name = test.name, tests = Seq(test), runPolicy = Tests.SubProcess(
              ForkOptions().withRunJVMOptions(Vector("-Xmx200m"))
            ))
          else
            Tests.Group(name = test.name, tests = Seq(test), runPolicy = Tests.SubProcess(ForkOptions()))
        }
      }
    )
    .dependsOn(`quill-zio` % "compile->compile;test->test")
    .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")
    .dependsOn(`quill-jdbc` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)



lazy val `quill-ndbc-monix` =
  (project in file("quill-ndbc-monix"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= Seq(
      )
    )
    .dependsOn(`quill-monix` % "compile->compile;test->test")
    .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")
    .dependsOn(`quill-ndbc` % "compile->compile;test->test")
    .dependsOn(`quill-ndbc-postgres` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)

lazy val `quill-spark` =
  (project in file("quill-spark"))
    .settings(commonNoLogSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= Seq(
        "org.apache.spark" %% "spark-sql" % "2.4.4"
      ),
      excludeDependencies ++= Seq(
        "ch.qos.logback"  % "logback-classic"
      )
    )
    .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)

lazy val `quill-finagle-mysql` =
  (project in file("quill-finagle-mysql"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= Seq(
        if (scalaVersion.value.startsWith("2.11"))
          "com.twitter" % "finagle-mysql_2.11" % "21.2.0"
        else
          "com.twitter" %% "finagle-mysql" % "21.9.0"
      )
    )
    .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)

lazy val `quill-finagle-postgres` =
  (project in file("quill-finagle-postgres"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= Seq(
        "io.github.finagle" %% "finagle-postgres" % "0.12.0"
      )
    )
    .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)

lazy val `quill-async` =
  (project in file("quill-async"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= Seq(
        "com.github.postgresql-async" %% "db-async-common"  % "0.3.0"
      )
    )
    .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)

lazy val `quill-async-mysql` =
  (project in file("quill-async-mysql"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= Seq(
        "com.github.postgresql-async" %% "mysql-async"      % "0.3.0"
      )
    )
    .dependsOn(`quill-async` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)

lazy val `quill-async-postgres` =
  (project in file("quill-async-postgres"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= Seq(
        "com.github.postgresql-async" %% "postgresql-async" % "0.3.0"
      )
    )
    .dependsOn(`quill-async` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)

lazy val `quill-jasync` =
  (project in file("quill-jasync"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= Seq(
        "com.github.jasync-sql" % "jasync-common" % "1.1.4",
        "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.1"
      )
    )
    .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)

lazy val `quill-jasync-postgres` =
  (project in file("quill-jasync-postgres"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= Seq(
        "com.github.jasync-sql" % "jasync-postgresql" % "1.1.4"
      )
    )
    .dependsOn(`quill-jasync` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)

lazy val `quill-jasync-mysql` =
  (project in file("quill-jasync-mysql"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= Seq(
        "com.github.jasync-sql" % "jasync-mysql" % "1.1.4"
      )
    )
    .dependsOn(`quill-jasync` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)

lazy val `quill-ndbc` =
  (project in file("quill-ndbc"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= Seq(
        "io.trane" % "future-scala" % "0.3.2",
        "io.trane" % "ndbc-core" % "0.1.3"
      )
    )
    .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)

lazy val `quill-ndbc-postgres` =
  (project in file("quill-ndbc-postgres"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= Seq(
        "io.trane" % "future-scala" % "0.3.2",
        "io.trane" % "ndbc-postgres-netty4" % "0.1.3"
      )
    )
    .dependsOn(`quill-ndbc` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)

lazy val `quill-cassandra` =
  (project in file("quill-cassandra"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= Seq(
        "com.datastax.cassandra" %  "cassandra-driver-core" % "3.7.2"
      )
    )
    .dependsOn(`quill-core-jvm` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)

lazy val `quill-cassandra-monix` =
  (project in file("quill-cassandra-monix"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true
    )
    .dependsOn(`quill-cassandra` % "compile->compile;test->test")
    .dependsOn(`quill-monix` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)

lazy val `quill-cassandra-zio` =
  (project in file("quill-cassandra-zio"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio" % "1.0.12",
        "dev.zio" %% "zio-streams" % "1.0.12",
        "dev.zio" %% "zio-interop-guava" % "31.0.0.0"
      )
    )
    .dependsOn(`quill-cassandra` % "compile->compile;test->test")
    .dependsOn(`quill-zio` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)


lazy val `quill-cassandra-lagom` =
   (project in file("quill-cassandra-lagom"))
    .settings(commonSettings: _*)
    .settings(mimaSettings: _*)
    .settings(
      Test / fork := true,
      libraryDependencies ++= {
        val lagomVersion = if (scalaVersion.value.startsWith("2.13")) "1.6.5" else "1.5.5"
        val versionSpecificDependencies =  if (scalaVersion.value.startsWith("2.13")) Seq("com.typesafe.play" %% "play-akka-http-server" % "2.8.8") else Seq.empty
        Seq(
          "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.4",
          "com.lightbend.lagom" %% "lagom-scaladsl-persistence-cassandra" % lagomVersion % Provided,
          "com.lightbend.lagom" %% "lagom-scaladsl-testkit" % lagomVersion % Test
        ) ++ versionSpecificDependencies
      }
    )
    .dependsOn(`quill-cassandra` % "compile->compile;test->test")
    .enablePlugins(MimaPlugin)


lazy val `quill-orientdb` =
  (project in file("quill-orientdb"))
      .settings(commonSettings: _*)
      .settings(mimaSettings: _*)
      .settings(
        Test / fork := true,
        libraryDependencies ++= Seq(
          "com.orientechnologies" % "orientdb-graphdb" % "3.0.39"
        )
      )
      .dependsOn(`quill-sql-jvm` % "compile->compile;test->test")
      .enablePlugins(MimaPlugin)

lazy val mimaSettings = Seq(
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

lazy val jdbcTestingLibraries = Seq(
  libraryDependencies ++= Seq(
    "com.zaxxer"              %  "HikariCP"                % "3.4.5",
    "mysql"                   %  "mysql-connector-java"    % "8.0.27"             % Test,
    "com.h2database"          %  "h2"                      % "1.4.200"            % Test,
    "org.postgresql"          %  "postgresql"              % "42.3.0"             % Test,
    "org.xerial"              %  "sqlite-jdbc"             % "3.36.0.3"             % Test,
    "com.microsoft.sqlserver" %  "mssql-jdbc"              % "7.1.1.jre8-preview" % Test,
    "com.oracle.ojdbc"        %  "ojdbc8"                  % "19.3.0.0"           % Test,
    "org.mockito"             %% "mockito-scala-scalatest" % "1.16.46"              % Test
  )
)

lazy val jdbcTestingSettings = jdbcTestingLibraries ++ Seq(
  Test / fork := true,
  unmanagedSources / excludeFilter := {
    excludeTests match {
      case true =>
        excludePaths((Test / unmanagedSourceDirectories).value.map(dir => dir.getCanonicalPath))
      case false =>
        excludePaths(List())
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

val scala_v_11 = "2.11.12"
val scala_v_12 = "2.12.10"
val scala_v_13 = "2.13.2"

lazy val loggingSettings = Seq(
  libraryDependencies ++= Seq(
    "ch.qos.logback"  % "logback-classic" % "1.2.6" % Test
  )
)

lazy val basicSettings = Seq(
  unmanagedSources / excludeFilter := {
    excludeTests match {
      case true  => excludePaths((Test / unmanagedSourceDirectories).value.map(dir => dir.getCanonicalPath))
      case false => new SimpleFileFilter(file => false)
    }
  },
  organization := "io.getquill",
  scalaVersion := scala_v_12,
  crossScalaVersions := Seq(scala_v_11, scala_v_12, scala_v_13),
  libraryDependencies ++= Seq(
    "org.scala-lang.modules" %%% "scala-collection-compat" % "2.2.0",
    "com.lihaoyi"     %% "pprint"         % pprintVersion(scalaVersion.value),
    "org.scalatest"   %%% "scalatest"     % "3.2.10"          % Test,
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
  Test / unmanagedClasspath ++= Seq(
    baseDirectory.value / "src" / "test" / "resources"
  ),
  EclipseKeys.eclipseOutput := Some("bin"),
  scalacOptions ++= Seq(
    "-target:jvm-1.8",
    "-encoding", "UTF-8",
    "-feature",
    "-deprecation",
    "-unchecked",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"

  ),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) =>
        Seq("-Ypatmat-exhaust-depth", "40")
      case Some((2, 11)) =>
        Seq("-Xlint",
          //"-Xfatal-warnings",
          "-Xfuture",
          "-deprecation",
          "-Yno-adapted-args",
          "-Ywarn-unused-import", "" +
          "-Xsource:2.12" // needed so existential types work correctly
        )
      case Some((2, 12)) =>
        Seq(
          //"-Xfatal-warnings",
          "-Xlint:-unused,_",
          "-Xfuture",
          "-deprecation",
          "-Yno-adapted-args",
          "-Ywarn-unused:imports",
          "-Ycache-macro-class-loader:last-modified"
        )
      case _ => Seq()
    }
  },
  Global / concurrentRestrictions += Tags.limit(Tags.Test, 1),
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

lazy val commonNoLogSettings = ReleasePlugin.extraReleaseCommands ++ basicSettings ++ releaseSettings
lazy val commonSettings = ReleasePlugin.extraReleaseCommands ++ basicSettings ++ loggingSettings ++ releaseSettings

lazy val releaseSettings = Seq(
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
  releaseVersionBump := sbtrelease.Version.Bump.Nano,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseProcess := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) =>
        doOnDefault(checkSnapshotDependencies) ++
          doOnDefault(inquireVersions) ++
          doOnDefault(runClean) ++
          doOnPush   (setReleaseVersion) ++
          doOnDefault(publishArtifacts)
      //doOnPush   ("sonatypeReleaseAll") ++
      case Some((2, 12)) =>
        doOnDefault(checkSnapshotDependencies) ++
        doOnDefault(inquireVersions) ++
        doOnDefault(runClean) ++
        doOnPush   (setReleaseVersion) ++
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
      case Some((2, 13)) =>
        doOnDefault(checkSnapshotDependencies) ++
        doOnDefault(inquireVersions) ++
        doOnDefault(runClean) ++
        doOnPush   (setReleaseVersion) ++
        doOnDefault(publishArtifacts)
        //doOnPush   ("sonatypeReleaseAll") ++
      case _ => Seq[ReleaseStep]()
    }
  },

  homepage := Some(url("http://github.com/getquill/quill")),
  licenses := List(("Apache License 2.0", url("https://raw.githubusercontent.com/getquill/quill/master/LICENSE.txt"))),
  developers := List(
    Developer("fwbrasil", "Flavio W. Brasil", "", url("http://github.com/fwbrasil")),
    Developer("deusaquilus", "Alexander Ioffe", "", url("https://github.com/deusaquilus"))
  ),
  scmInfo := Some(
    ScmInfo(url("https://github.com/getquill/quill"), "git:git@github.com:getquill/quill.git")
  ),

)
