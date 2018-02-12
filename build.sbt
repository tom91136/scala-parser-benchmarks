
enablePlugins(JmhPlugin)

lazy val JmhVersion = "1.19"

lazy val `parsec-for-scala` = project.settings(
	name := "parsec-for-scala",
	version := "0.1.0-SNAPSHOT",
	scalaVersion := "2.12.4",
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

lazy val `fastparse-benchmark` = (project in file(".")).settings(
	organization := "net.kurobako",
	name := "fastparse-benchmark",
	version := "0.1.0-SNAPSHOT",
	scalaVersion := "2.12.4",
	scalacOptions ++= Seq(
		"-target:jvm-1.8",
		"-encoding", "UTF-8",
		"-unchecked",
		"-deprecation",
		"-Xfuture",
		"-Ywarn-dead-code",
		"-Ywarn-numeric-widen",
		"-Ywarn-value-discard",
		"-Ywarn-unused",
		"-Ypartial-unification",
		//			"-Xlog-implicits",
	),
	javacOptions ++= Seq(
		"-target", "1.8",
		"-source", "1.8",
		"-Xlint:deprecation"),
	libraryDependencies ++= Seq(
		"com.lihaoyi" %% "fastparse" % "1.0.0",
		"org.openjdk.jmh" % "jmh-core" % JmhVersion,
		"org.openjdk.jmh" % "jmh-generator-annprocess" % JmhVersion % Compile,
		"org.scalatest" %% "scalatest" % "3.0.1" % Test
	),
	mainClass in Compile := Some("net.kurobako.fastparsebench.FastparseBenchmark"),
)
	.aggregate(`parsec-for-scala`)
	.dependsOn(`parsec-for-scala`)


