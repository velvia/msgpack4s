import ReleaseTransformations._

name := "msgpack4s"

val commonSettings = Seq(
  scalaVersion := "2.12.0",
  organization := "org.velvia",
  crossScalaVersions := Seq("2.11.7", "2.12.0")
)

unmanagedSourceDirectories in Compile <++= Seq(baseDirectory(_ / "src" )).join

unmanagedSourceDirectories in Test <++= Seq(baseDirectory(_ / "test" )).join

// Testing deps
libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "3.0.0" % "test",
                            "org.mockito" % "mockito-all" % "1.9.0" % "test")

lazy val rojomaJson = "com.rojoma" %% "rojoma-json-v3" % "3.7.0"
lazy val json4s     = "org.json4s" %% "json4s-native" % "3.5.0"
lazy val commonsIo  = "org.apache.commons" % "commons-io" % "1.3.2"
lazy val playJson   = "com.typesafe.play" %% "play-json" % "2.6.0-M1"

// Extra dependencies for type classes for JSON libraries
libraryDependencies ++= Seq(rojomaJson % "provided",
                            json4s     % "provided",
                            playJson   % "provided")

licenses += ("Apache-2.0", url("http://choosealicense.com/licenses/apache/"))

// POM settings for Sonatype
homepage := Some(url("https://github.com/velvia/msgpack4s"))

scmInfo := Some(ScmInfo(url("https://github.com/velvia/msgpack4s"),
                            "git@github.com:velvia/msgpack4s.git"))

developers := List(Developer("velvia",
                        "Evan Chan",
                        "velvia@gmail.com",
                        url("https://github.com/velvia")))

pomIncludeRepository := (_ => false)

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
)

lazy val msgpack4s = (project in file(".")).settings(commonSettings: _*)

lazy val jmh = (project in file("jmh")).dependsOn(msgpack4s)
                        .settings(commonSettings: _*)
                        .settings(jmhSettings: _*)
                        .settings(libraryDependencies += rojomaJson)
                        .settings(libraryDependencies += json4s)
                        .settings(libraryDependencies += commonsIo)
