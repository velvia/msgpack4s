name := "msgpack4s"

// Remove -SNAPSHOT from the version before publishing a release. Don't forget to change the version to
// $(NEXT_VERSION)-SNAPSHOT afterwards!
version := "0.1-SNAPSHOT"

organization := "com.github.velvia"

scalaVersion := "2.9.2"

unmanagedSourceDirectories in Compile <++= Seq(baseDirectory(_ / "src" )).join

unmanagedSourceDirectories in Test <++= Seq(baseDirectory(_ / "test" )).join

// Testing deps
libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "1.9.1" % "test",
                            "org.mockito" % "mockito-all" % "1.9.0" % "test")
