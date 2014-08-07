package services

import models.GroceryList
import play.api.Logger
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import scala.concurrent.Future

class GroceryListService extends MongoDataSource {
  lazy val groceryLists: BSONCollection = db("grocery_lists")

  import models.BSONHandlers._

  def findGroceryLists = {
    groceryLists
      .find(BSONDocument())
      .sort(BSONDocument("name" -> 1))
      .cursor[GroceryList]
      .collect[List]()
  }

  def findGroceryList(name: String) = {
    val futureGroceryLists = groceryLists
      .find(BSONDocument("name" -> name))
      .cursor[GroceryList]
      .collect[List]()

    futureGroceryLists flatMap { groceryLists =>
      if (groceryLists.length > 1) Future.failed(new IllegalStateException(s"found ${groceryLists.length} grocery lists with name $name, there can be only one"))
      else Future.successful(groceryLists.headOption)
    }
  }

  def insertGroceryList(groceryList: GroceryList) = {
    findGroceryList(groceryList.name).flatMap {
      case None => {
        groceryLists.insert(groceryList).flatMap { lastError =>
          Logger.debug(s"inserted ${groceryList.name} with LastError: $lastError")
          if (lastError.ok)
            Future.successful(groceryList)
          else {
            Logger.error(s"insert failed: ${lastError.err.getOrElse("")}")
            Future.failed(lastError)
          }
        }
      }
      case Some(l) => Future.failed(new IllegalArgumentException(s"a grocery list already exists named [${l.name}]"))
    }
  }

  def updateGroceryList(groceryList: GroceryList) = {
    groceryLists
      .update(BSONDocument("name" -> groceryList.name), groceryList)
      .flatMap { lastError =>
        Logger.debug(s"updated ${groceryList.name} with LastError: $lastError")
        if (lastError.ok)
          Future.successful(groceryList)
        else {
          Logger.error(s"update failed: ${lastError.err.getOrElse("")}")
          Future.failed(lastError)
        }
      }
  }
}
