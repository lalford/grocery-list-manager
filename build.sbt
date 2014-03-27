import play.Project._

name := "RecipeManager"

version := "1.0"

playScalaSettings

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2"
)