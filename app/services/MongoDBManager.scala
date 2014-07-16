package services

import reactivemongo.api.{DB, DefaultDB, MongoDriver}
import reactivemongo.core.nodeset.Authenticate
import scala.concurrent.ExecutionContext

object MongoDBManager {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  lazy val driver = new MongoDriver
  lazy val connection = driver.connection(Seq("localhost"), Seq(Authenticate("recipe-manager", "manager", "manager!")))

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