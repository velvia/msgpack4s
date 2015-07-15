import bintray.Plugin.bintrayPublishSettings

name := "msgpack4s"

// Remove -SNAPSHOT from the version before publishing a release. Don't forget to change the version to
// $(NEXT_VERSION)-SNAPSHOT afterwards!
version := "0.5.0"

organization := "org.velvia"

scalaVersion := "2.10.4"

crossScalaVersions := Seq("2.10.4", "2.11.5")

unmanagedSourceDirectories in Compile <++= Seq(baseDirectory(_ / "src" )).join

unmanagedSourceDirectories in Test <++= Seq(baseDirectory(_ / "test" )).join

// Testing deps
libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "2.2.0" % "test",
                            "org.mockito" % "mockito-all" % "1.9.0" % "test")

lazy val rojomaJson = "com.rojoma" %% "rojoma-json-v3" % "3.3.0"
lazy val json4s     = "org.json4s" %% "json4s-native" % "3.2.11"
lazy val commonsIo  = "org.apache.commons" % "commons-io" % "1.3.2"

// Extra dependencies for type classes for JSON libraries
libraryDependencies ++= Seq(rojomaJson % "provided",
                            json4s     % "provided")

Seq(bintrayPublishSettings: _*)

licenses += ("Apache-2.0", url("http://choosealicense.com/licenses/apache/"))

lazy val msgpack4s = (project in file("."))

lazy val jmh = (project in file("jmh")).dependsOn(msgpack4s)
                        .settings(jmhSettings:_*)
                        .settings(libraryDependencies += rojomaJson)
                        .settings(libraryDependencies += json4s)
                        .settings(libraryDependencies += commonsIo)
