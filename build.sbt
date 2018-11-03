import sbt.Keys.scalaVersion

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

lazy val `scala-parser-benchmarks` = (project in file(".")).settings(
	organization := "net.kurobako",
	name := "scala-parser-benchmarks",
	version := "0.1.0-SNAPSHOT",
	commonSettings,
	scalacLintAll,
	resolvers += "bintray-djspiewak-maven" at "https://dl.bintray.com/djspiewak/maven",
	resolvers += "jitpack" at "https://jitpack.io",
	libraryDependencies ++= Seq(

		// parser dependencies
		"org.tpolecat" %% "atto-core" % "0.6.4",
		"org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.1",
		"com.github.meerkat-parser" % "Meerkat" % "3e59173f1e",
		"org.parboiled" %% "parboiled" % "2.1.5",
		"com.lihaoyi" %% "fastparse" % "1.0.0",
		"com.codecommit" %% "parseback-core" % "0.4.0-f0c3683",
		"com.codecommit" %% "parseback-cats" % "0.3",


		"org.openjdk.jmh" % "jmh-core" % JmhVersion,
		"org.openjdk.jmh" % "jmh-generator-annprocess" % JmhVersion % Compile,

		"org.typelevel" %% "cats-core" % "1.4.0",
		"org.scalatest" %% "scalatest" % "3.0.1" % Test
	),
	mainClass in Compile := Some("net.kurobako.spb.Simple"),
//	assemblyShadeRules in assembly := Seq(
//		ShadeRule.rename("fastparse.**" -> "fastparse_shaded_v1")
//			.inLibrary("com.lihaoyi" %% "fastparse" % "1.0.0")
//			.inProject
//	)

)
	.aggregate(`parsec-for-scala`, parsley)
	.dependsOn(`parsec-for-scala`, parsley)


