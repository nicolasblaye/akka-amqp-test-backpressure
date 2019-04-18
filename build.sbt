name := "akka-amqp-example"

version := "0.1"

scalaVersion := "2.12.8"

val log4j2 = "2.11.1"

libraryDependencies ++= Seq(
  "com.lightbend.akka" %% "akka-stream-alpakka-amqp" % "1.0.0",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.21",
  "org.apache.logging.log4j" % "log4j-api" % log4j2,
  "org.apache.logging.log4j" % "log4j-core" % log4j2,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j2
)

enablePlugins(JavaAppPackaging)