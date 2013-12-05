name := "roshan"

version := "1.0"

scalaVersion := "2.10.3"

// Dependancies --------------
// Akka
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
 
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.2.3"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.2.3"

// Parboiled
// Included in another library here and conflicts (apparently)
// libraryDependencies += "org.parboiled" % "parboiled-scala"

// ScalaTest
libraryDependencies += "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"

// Simple Logging
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.4"

// Slick
libraryDependencies += "com.typesafe.slick" %% "slick" % "1.0.0"

// Protobuf
libraryDependencies += "com.google.protobuf" % "protobuf-java" % "2.5.0"

// Spray
resolvers += "spray" at "http://repo.spray.io/"

libraryDependencies += "io.spray" %%  "spray-json" % "1.2.4" 

// Sqlite
libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.7.2"

libraryDependencies += "commons-lang" % "commons-lang" % "2.6"
