lazy val akkaHttpVersion = "10.1.5"
lazy val akkaVersion    = "2.5.17"
lazy val doobieVersion    = "0.5.3"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "io.github.mlypik",
      scalaVersion    := "2.12.7"
    )),
    name := "money-transfers",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,

      "org.tpolecat"      %% "doobie-core"          % doobieVersion,
      "org.tpolecat"      %% "doobie-h2"            % doobieVersion,
      "org.tpolecat"      %% "doobie-specs2"        % doobieVersion,

      "com.h2database"    %% "h2"                   % "1.4.197",

      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.5"         % Test
    )
  )
