package services

import play.api.Logger
import reactivemongo.api.{MongoConnection, DB, DefaultDB, MongoDriver}
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object MongoDBManager {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  val parsedUri = {
    val env = System.getenv("MONGOHQ_URL")
    val url = {
      if (env == null || env.isEmpty)
        "mongodb://manager:manager!@localhost:27017/recipe-manager"
      else
        env
    }

    MongoConnection.parseURI(url) match {
      case Success(uri) =>
        Logger.info(s"mongo url = $url, parsed uri = $uri")
        assert(uri.db.isDefined)
        uri
      case Failure(t) =>
        Logger.error("could not parse mongodb url", t)
        throw new RuntimeException(t)
    }
  }

  lazy val driver = new MongoDriver
  lazy val connection = driver.connection(parsedUri)

  def dbFactory: DefaultDB = DB(parsedUri.db.get, connection)

  // just used to kick the connection/authenticate process at app start
  def connect {
    connection
  }
}

trait MongoDataSource {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  lazy val db = MongoDBManager.dbFactory
}