name := "akka_first"

version := "1.0"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.8",
  "com.codemettle.akka-solr" %% "akka-solr" % "0.10.2",
  "org.apache.solr" % "solr-solrj" % "4.10.3"
)
