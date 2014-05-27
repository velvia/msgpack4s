import bintray.Plugin.bintrayPublishSettings

name := "msgpack4s"

// Remove -SNAPSHOT from the version before publishing a release. Don't forget to change the version to
// $(NEXT_VERSION)-SNAPSHOT afterwards!
version := "0.4.2-SNAPSHOT"

organization := "org.velvia"

scalaVersion := "2.10.4"

crossScalaVersions := Seq("2.9.2", "2.10.4")

unmanagedSourceDirectories in Compile <++= Seq(baseDirectory(_ / "src" )).join

unmanagedSourceDirectories in Test <++= Seq(baseDirectory(_ / "test" )).join

// Testing deps
libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "1.9.1" % "test",
                            "org.mockito" % "mockito-all" % "1.9.0" % "test")

Seq(bintrayPublishSettings: _*)

licenses += ("Apache-2.0", url("http://choosealicense.com/licenses/apache/"))