import play.Project._

name := "RecipeManager"

version := "1.0"

playScalaSettings

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % "0.10.0"
)