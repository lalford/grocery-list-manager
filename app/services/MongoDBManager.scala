package services

import play.api.Logger
import reactivemongo.api.{MongoConnection, DB, DefaultDB, MongoDriver}
import reactivemongo.core.nodeset.Authenticate
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object MongoDBManager {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  lazy val driver = new MongoDriver
  lazy val connection = {
    val mongoHqUrl = System.getenv("MONGOHQ_URL")
    if (mongoHqUrl == null || mongoHqUrl.isEmpty)
      driver.connection(Seq("localhost"), Seq(Authenticate("recipe-manager", "manager", "manager!")))
    else
      MongoConnection.parseURI(mongoHqUrl) match {
        case Success(uri) => driver.connection(uri)
        case Failure(t) =>
          Logger.error("could not parse mongohq url", t)
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