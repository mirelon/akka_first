name := "akka_first"

version := "1.0"

scalaVersion := "2.11.5"

val akkaVersion = "2.3.8"
val sprayVersion = "1.3.2"

// %% is for cross-building, i.e. the library will have appended scala version to its name

libraryDependencies ++= Seq(
  "com.typesafe.akka"        %% "akka-actor" % akkaVersion,
  "com.typesafe.akka"        %% "akka-slf4j" % akkaVersion,
  "com.codemettle.akka-solr" %% "akka-solr" % "0.10.2",
  "io.spray"                 %% "spray-can" % sprayVersion,
  "io.spray"                 %% "spray-routing" % sprayVersion,
  "io.spray"                 %% "spray-json" % "1.3.1",
  "io.spray"                 %% "spray-client" % sprayVersion,
  "org.apache.solr"           % "solr-solrj" % "4.10.3",
  "net.virtual-void"         %% "json-lenses" % "0.6.0"
)
