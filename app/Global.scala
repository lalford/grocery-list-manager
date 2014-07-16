import play.api.{Mode, Application, GlobalSettings}
import services.MongoDBManager
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Global extends GlobalSettings {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  override def beforeStart(app: Application) {
    super.beforeStart(app)
    if(app.mode != Mode.Test) MongoDBManager.connect
  }

  override def onStop(app: Application) {
    super.onStop(app)
    if(app.mode != Mode.Test)
      MongoDBManager.connection.askClose()(10.seconds).onComplete {
        case e => {
          MongoDBManager.driver.close
        }
      }
  }
}
