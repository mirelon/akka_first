name := "akka_first"

version := "1.0"

scalaVersion := "2.11.4"

val akkaVersion = "2.3.8"
val sprayVersion = "1.3.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "io.spray" %% "spray-can" % sprayVersion,
  "io.spray" %% "spray-routing" % sprayVersion,
  "io.spray" %% "spray-json" % "1.3.1",
  "com.codemettle.akka-solr" %% "akka-solr" % "0.10.2",
  "org.apache.solr" % "solr-solrj" % "4.10.3"
)
