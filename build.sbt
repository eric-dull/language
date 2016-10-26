organization := "network.eic.language"

name := "language"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers += "Akka Repository" at "http://repo.akka.io/releases/"

resolvers += "typesafe" at "http://repo.typesafe.com/typesafe/releases/"
/*
classPath += "/Users/nbarthedejean/Code/lib/stanford-corenlp/stanford-corenlp-3.6.0.jar"

classPath += "/Users/nbarthedejean/Code/lib/slf4j-1.7.21/slf4j-simple-1.7.21.jar"*/

libraryDependencies ++= Seq(
  "com.rockymadden.stringmetric" %% "stringmetric-core" % "0.27.4",
  "javax.mail" % "javax.mail-api" % "1.5.5",
  "com.sun.mail" % "javax.mail" % "1.5.5",
  "org.apache.spark" %% "spark-core" % "1.6.1",
  "org.reactivemongo" %% "reactivemongo" % "0.11.11",
  "org.mongodb" %% "casbah" % "3.1.1",
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.typesafe" % "config" % "1.3.0",
  "net.debasishg" %% "redisclient" % "3.0",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0",
  "com.google.protobuf" % "protobuf-java" % "3.1.0",
  "com.tumblr" %% "colossus" % "0.8.1",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.2",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.2.2"
)

