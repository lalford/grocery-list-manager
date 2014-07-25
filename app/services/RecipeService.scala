package services

import models.Recipe
import play.api.Logger
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument

import scala.concurrent.Future

class RecipeService extends MongoDataSource {
  override val dbName = "recipe-manager"
  lazy val recipes: BSONCollection = db("recipes")

  import models.BSONHandlers._

  def findRecipes = {
    recipes
      .find(BSONDocument())
      .sort(BSONDocument("name" -> 1))
      .cursor[Recipe]
      .collect[List]()
  }

  def findRecipe(name: String) = {
    val futureRecipes =recipes
      .find(BSONDocument("name" -> name))
      .cursor[Recipe]
      .collect[List]()

    futureRecipes flatMap { recipes =>
      if (recipes.length > 1) Future.failed(new IllegalStateException(s"found ${recipes.length} recipes with name $name, there can be only one"))
      else Future.successful(recipes.headOption)
    }
  }

  def insertRecipe(recipe: Recipe) = {
    findRecipe(recipe.name) flatMap {
      case None => {
        recipes.insert(recipe).flatMap { lastError =>
          Logger.debug(s"inserted with LastError: $lastError")
          if (lastError.ok)
            Future.successful(recipe)
          else {
            Logger.error(s"insert failed: ${lastError.err.getOrElse("")}")
            Future.failed(lastError)
          }
        }
      }
      case Some(r) => Future.failed(new IllegalArgumentException(s"a recipe already exists named [${r.name}]"))
    }
  }

  def updateRecipe(recipe: Recipe) = {
    recipes
      .update(BSONDocument("name" -> recipe.name), recipe)
      .flatMap { lastError =>
        Logger.debug(s"updated ${recipe.name} with LastError: $lastError")
        if (lastError.ok)
          Future.successful(recipe)
        else {
          Logger.error(s"update failed: ${lastError.err.getOrElse("")}")
          Future.failed(lastError)
        }
    }
  }
}
