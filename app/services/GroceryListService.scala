package services

import models.GroceryList
import play.api.Logger
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import scala.concurrent.{Promise, Future}
import scala.util.{Failure, Success}

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

  def deleteGroceryList(name: String) = {
    groceryLists
    .remove(BSONDocument("name" -> name))
    .flatMap { lastError =>
      Logger.debug(s"removed $name with LastError: $lastError")
      if (lastError.ok)
        Future.successful(name)
      else {
        Logger.error(s"remove failed: ${lastError.err.getOrElse("")}")
        Future.failed(lastError)
      }
    }
  }

  def deleteRecipeFromGroceryLists(name: String) = {
    findGroceryLists flatMap { allGroceryLists =>
      Future.traverse(allGroceryLists) { groceryList =>
        val updatePromise = Promise[Option[String]]()

        if (groceryList.recipeServings.exists(rs => rs.name == name)) {
          val cleanedRS = groceryList.recipeServings.filterNot(rs => rs.name == name)

          updateGroceryList(groceryList.copy(recipeServings = cleanedRS)) onComplete {
            case Success(_) =>
              updatePromise.success(Some(groceryList.name))
            case Failure(t) =>
              Logger.error(s"failed to remove included recipe $name! mongo problem?", t)
              updatePromise.success(Some(groceryList.name))
          }
        } else
          updatePromise.success(None)

        updatePromise.future
      }
    }
  }
}
