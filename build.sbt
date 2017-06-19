name := "Akka tutorial"

version := "1.0"

scalaVersion := "2.11.6"

resolvers +=  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.2"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.2" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3" % "test"
