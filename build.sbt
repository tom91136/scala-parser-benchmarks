import sbt.Keys.{libraryDependencies, scalaVersion}

enablePlugins(JmhPlugin)


lazy val commonSettings = Seq(
	scalaVersion := "2.12.7",
	javacOptions ++= Seq(
		"-target", "1.8",
		"-source", "1.8",
		"-Xlint:deprecation")
)

lazy val scalacLintAll = Seq(
	scalacOptions ++= Seq(
		"-target:jvm-1.8",
		"-encoding", "UTF-8",
		"-unchecked",
		"-deprecation",
		"-explaintypes",
		"-feature",
		"-Xfuture",

		"-language:existentials",
		"-language:experimental.macros",
		"-language:higherKinds",
		"-language:postfixOps",
		"-language:implicitConversions",

		"-Xlint:adapted-args",
		"-Xlint:by-name-right-associative",
		"-Xlint:constant",
		"-Xlint:delayedinit-select",
		"-Xlint:doc-detached",
		"-Xlint:inaccessible",
		"-Xlint:infer-any",
		"-Xlint:missing-interpolator",
		"-Xlint:nullary-override",
		"-Xlint:nullary-unit",
		"-Xlint:option-implicit",
		"-Xlint:package-object-classes", // too widespread
		"-Xlint:poly-implicit-overload",
		"-Xlint:private-shadow",
		"-Xlint:stars-align",
		"-Xlint:type-parameter-shadow",
		"-Xlint:unsound-match",

		"-Yno-adapted-args",
		"-Ywarn-dead-code",
		"-Ywarn-extra-implicit",
		"-Ywarn-inaccessible",
		"-Ywarn-infer-any",
		"-Ywarn-nullary-override",
		"-Ywarn-nullary-unit",
		"-Ywarn-numeric-widen",
		"-Ywarn-unused:implicits",
		//		"-Ywarn-unused:imports",
		"-Ywarn-unused:locals",
		"-Ywarn-unused:params",
		"-Ywarn-unused:patvars",
		"-Ywarn-unused:privates",
		"-Ywarn-value-discard",
		"-Ypartial-unification",
	),
)


lazy val `parsec-for-scala` = project.settings(
	name := "parsec-for-scala",
	version := "0.1.0-SNAPSHOT",
	commonSettings,
	scalacOptions ++= Seq(
		"-target:jvm-1.8",
		"-encoding", "UTF-8",
		"-unchecked",
		"-deprecation",
		"-Xfuture",
		"-Ypartial-unification",
		//			"-Xlog-implicits",
	),
	scalaSource in Compile := baseDirectory.value / "src"
)

lazy val parsley = project.settings(
	name := "parsley",
	version := "0.1.0-SNAPSHOT",
	commonSettings,
	scalacOptions ++= Seq(
		"-opt:l:inline",
		"-opt-inline-from"
	),
	scalaSource in Compile := baseDirectory.value / "src",
	scalaSource in Test := baseDirectory.value / "test",
	libraryDependencies ++= Seq(
		"org.scalatest" %% "scalatest" % "3.0.1" % Test
	),
)


lazy val JmhVersion = "1.21"

lazy val localProjectSettings = Seq(
	organization := "net.kurobako",
	version := "0.1.0-SNAPSHOT",
) ++ commonSettings ++ scalacLintAll


lazy val TestDependencies = Seq("org.scalatest" %% "scalatest" % "3.0.1" % Test)
lazy val JmhDependencies = Seq(
	"org.openjdk.jmh" % "jmh-core" % JmhVersion,
	"org.openjdk.jmh" % "jmh-generator-annprocess" % JmhVersion % Compile,
)

lazy val fixtures = project.settings(
	localProjectSettings,
	libraryDependencies ++= JmhDependencies
)

lazy val BenchDependencies = TestDependencies ++ JmhDependencies

lazy val `baseline-recursive-descent-bench` = project.settings(
	localProjectSettings,
	libraryDependencies ++= BenchDependencies,
).dependsOn(fixtures).enablePlugins(JmhPlugin)

lazy val `parsley-bench` = project.settings(
	localProjectSettings,
	libraryDependencies ++= BenchDependencies,
).dependsOn(parsley, fixtures).enablePlugins(JmhPlugin)

lazy val `parsec-for-scala-bench` = project.settings(
	localProjectSettings,
	libraryDependencies ++= BenchDependencies,
).dependsOn(`parsec-for-scala`, fixtures).enablePlugins(JmhPlugin)

lazy val `fastparse1-bench` = project.settings(
	localProjectSettings,
	libraryDependencies ++=
	Seq("com.lihaoyi" %% "fastparse" % "1.0.0") ++ BenchDependencies,
	//	logLevel in assembly := Level.Debug,
	//	assemblyShadeRules in assembly := Seq(
	//		ShadeRule.rename("fastparse.**" -> "fastparse_shaded.@1").inAll
	//			.inLibrary("com.lihaoyi" %% "fastparse" % "1.0.0")
	//			.inProject
	//	)
).dependsOn(fixtures).enablePlugins(JmhPlugin)

lazy val `fastparse2-bench` = project.settings(
	localProjectSettings,
	libraryDependencies ++=
	Seq("com.lihaoyi" %% "fastparse" % "2.0.4") ++ BenchDependencies,
	test in assembly := {},
).dependsOn(fixtures).enablePlugins(JmhPlugin)

lazy val `parboiled2-bench` = project.settings(
	localProjectSettings,
	libraryDependencies ++=
	Seq("org.parboiled" %% "parboiled" % "2.1.5") ++ BenchDependencies,
).dependsOn(fixtures).enablePlugins(JmhPlugin)

lazy val `parseback-bench` = project.settings(
	localProjectSettings,
	resolvers += "bintray-djspiewak-maven" at "https://dl.bintray.com/djspiewak/maven",
	libraryDependencies ++=
	Seq("com.codecommit" %% "parseback-core" % "0.4.0-f0c3683",
		"com.codecommit" %% "parseback-cats" % "0.3") ++ BenchDependencies,
).dependsOn(fixtures).enablePlugins(JmhPlugin)

lazy val `atto-bench` = project.settings(
	localProjectSettings,
	libraryDependencies ++=
	Seq("org.tpolecat" %% "atto-core" % "0.6.4") ++ BenchDependencies,
).dependsOn(fixtures).enablePlugins(JmhPlugin)

lazy val `scala-parser-combinators-bench` = project.settings(
	localProjectSettings,
	libraryDependencies ++=
	Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.1") ++ BenchDependencies,
).dependsOn(fixtures).enablePlugins(JmhPlugin)

lazy val `meerkat-bench` = project.settings(
	localProjectSettings,
	resolvers += "jitpack" at "https://jitpack.io",
	libraryDependencies ++=
	Seq("com.github.meerkat-parser" % "Meerkat" % "3e59173f1e") ++ BenchDependencies,
).dependsOn(fixtures).enablePlugins(JmhPlugin)

lazy val AllBenches = Seq(
	`parsley-bench`,
	`fastparse1-bench`,
	`fastparse2-bench`,
	`parsec-for-scala-bench`,
	`parboiled2-bench`,
	`parseback-bench`,
	`atto-bench`,
	`scala-parser-combinators-bench`,
	`baseline-recursive-descent-bench`,
	`meerkat-bench`,
)

val benchmarkAll = taskKey[String]("Run all individual benchmarks")

def mkJmhArgs(iteration: Int = 10,
			  warmup: Int = 10,
			  fork: Int = 1,
			  thread: Int = 1,
			  format: String = "json",
			  filename: String) = Seq("i" -> iteration,
	"wi" -> warmup,
	"f" -> fork,
	"t" -> thread,
	"rf" -> format,
	"rff" -> filename)
	.map { case (flag, v) => s"-$flag $v" }.mkString(" ")


lazy val `scala-parser-benchmarks` = (project in file(".")).settings(
	name := "scala-parser-benchmarks",
	localProjectSettings,
	libraryDependencies ++= BenchDependencies,
	cancelable in Global := true,
	benchmarkAll := Def.taskDyn {
		Def.sequential(
			clean +:
			AllBenches.map(project => Def.sequential(((Jmh / run) in project)
				.toTask(" " + mkJmhArgs(
					//					iteration = 5,
					//					warmup = 5,
					filename = s"../docs/jmh_${project.id}.json")))),
			Def.task("Done"))
	}.value,
	//	mainClass in Compile := Some("net.kurobako.spb.BenchmarkAll"),
	//	mainClass in(Jmh, run) := Some("net.kurobako.spb.BenchmarkAll"),
)
	.aggregate(AllBenches.map(Project.projectToLocalProject(_)): _*)
	//	.dependsOn(AllBenches.map(classpathDependency(_)): _*)
	.enablePlugins(JmhPlugin)


