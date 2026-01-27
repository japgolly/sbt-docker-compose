name := "sbt-docker-compose-env"

organization := "com.github.japgolly.sbt-docker-compose-env"

version := "0.1.0-SNAPSHOT"

// MANDATORY: sbt 1.x plugins and build libraries must use Scala 2.12
scalaVersion := "2.12.21"

libraryDependencies +=  "org.scala-sbt" % "sbt" % "1.11.7" % Provided
