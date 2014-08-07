package services

import play.api.Logger
import reactivemongo.api.{MongoConnection, DB, DefaultDB, MongoDriver}
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object MongoDBManager {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  lazy val driver = new MongoDriver
  lazy val connection = {
    val url = {
      val env = System.getenv("MONGOHQ_URL")
      if (env == null || env.isEmpty)
        s"mongodb://manager:manager!@localhost:27017/recipe-manager"
      else
        env
    }

    MongoConnection.parseURI(url) match {
      case Success(uri) =>
        Logger.info(s"mongo url = $url, parsed uri = $uri")
        println(s"mongo url = $url, parsed uri = $uri")
        driver.connection(uri)
      case Failure(t) =>
        Logger.error("could not parse mongodb url", t)
        throw t
    }
  }

  def dbFactory(name: String): DefaultDB = DB(name, connection)

  // just used to kick the connection/authenticate process at app start
  def connect {
    connection
  }
}

trait MongoDataSource {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val dbName: String
  lazy val db = MongoDBManager.dbFactory(dbName)
}