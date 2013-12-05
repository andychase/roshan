name := "roshan"

version := "1.0"

scalaVersion := "2.10.3"

// Dependancies --------------
// Akka
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
 
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.4"

// Parboiled
libraryDependencies += "org.parboiled" % "parboiled-scala" % "1.0.1"

// ScalaTest
libraryDependencies += "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"

// Simple Logging
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.4"

// Slick
libraryDependencies += "com.typesafe.slick" %% "slick" % "1.0.0"

// Spray
resolvers += "spray" at "http://repo.spray.io/"

libraryDependencies += "io.spray" %%  "spray-json" % "1.2.4" 

// Sqlite
libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.7.2"
            
// OneJar
seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

libraryDependencies += "commons-lang" % "commons-lang" % "2.6"
