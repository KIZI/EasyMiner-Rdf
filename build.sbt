name := "easyminer-rdf"

version := "1.0.0"

scalaVersion := "2.12.3"

organization := "cz.vse.easyminer"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8" /*, "-Xlog-implicits"*/)

val akkaV = "2.5.11"
val akkaHttpV = "10.1.0"
val slf4jV = "1.7.25"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaV
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaV
libraryDependencies += "com.typesafe.akka" %% "akka-http" % akkaHttpV
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV
libraryDependencies += "com.github.kxbmap" %% "configs" % "0.4.4"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaV
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"
libraryDependencies += "org.slf4j" % "slf4j-simple" % slf4jV
libraryDependencies += "org.slf4j" % "log4j-over-slf4j" % slf4jV
libraryDependencies += "com.github.propi.rdfrules" %% "core" % "1.0.0"

enablePlugins(PackPlugin)