import bintray.Plugin.bintrayPublishSettings

name := "msgpack4s"

val commonSettings = Seq(scalaVersion := "2.11.7", organization := "org.velvia", crossScalaVersions := Seq("2.10.4", "2.11.7"))

unmanagedSourceDirectories in Compile <++= Seq(baseDirectory(_ / "src" )).join

unmanagedSourceDirectories in Test <++= Seq(baseDirectory(_ / "test" )).join

// Testing deps
libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "2.2.0" % "test",
                            "org.mockito" % "mockito-all" % "1.9.0" % "test")

lazy val rojomaJson = "com.rojoma" %% "rojoma-json-v3" % "3.3.0"
lazy val json4s     = "org.json4s" %% "json4s-native" % "3.2.11"
lazy val commonsIo  = "org.apache.commons" % "commons-io" % "1.3.2"
lazy val playJson   = "com.typesafe.play" %% "play-json" % "2.4.1"

// Extra dependencies for type classes for JSON libraries
libraryDependencies ++= Seq(rojomaJson % "provided",
                            json4s     % "provided",
                            playJson   % "provided")

Seq(bintrayPublishSettings: _*)

licenses += ("Apache-2.0", url("http://choosealicense.com/licenses/apache/"))

lazy val msgpack4s = (project in file(".")).settings(commonSettings: _*)

lazy val jmh = (project in file("jmh")).dependsOn(msgpack4s)
                        .settings(commonSettings: _*)
                        .settings(jmhSettings: _*)
                        .settings(libraryDependencies += rojomaJson)
                        .settings(libraryDependencies += json4s)
                        .settings(libraryDependencies += commonsIo)
