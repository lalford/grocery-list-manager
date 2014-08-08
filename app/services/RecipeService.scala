package services

import models.Recipe
import play.api.Logger
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument

import scala.concurrent.{Promise, Future}
import scala.util.{Failure, Success}

class RecipeService extends MongoDataSource {
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
      case None => checkEmbeddedRecipes(recipe) {
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

  def updateRecipe(recipe: Recipe) = checkEmbeddedRecipes(recipe) {
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

  def deleteRecipeFromOtherRecipes(name: String): Future[List[Option[String]]] = {
    findRecipes flatMap { allRecipes =>
      Future.traverse(allRecipes) { recipe =>
        val updatePromise = Promise[Option[String]]()

        if (recipe.recipeIngredients.exists(ri => ri.name == name)) {
          val cleanedRI = recipe.recipeIngredients.filterNot(ri => ri.name == name)

          updateRecipe(recipe.copy(recipeIngredients = cleanedRI)) onComplete {
            case Success(_) =>
              updatePromise.success(Some(recipe.name))
            case Failure(t) =>
              Logger.error(s"failed to remove included recipe $name! mongo problem?", t)
              updatePromise.success(Some(recipe.name))
          }
        } else
          updatePromise.success(None)

        updatePromise.future
      }
    }
  }

  def deleteRecipe(name: String) = {
    recipes
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

  private def checkEmbeddedRecipes(recipe: Recipe)(persistRecipe: => Future[Recipe]) = {
    val recipeNames = recipe.recipeIngredients.map(_.name)
    val checkedEmbeddedRecipesFutures = recipeNames map { name =>
      val existingRecipesFuture = Future.traverse(recipeNames) { name => findRecipe(name) }
      existingRecipesFuture flatMap { existingRecipes =>
        if (existingRecipes.contains(None)) Future.failed(new IllegalArgumentException(s"embedded recipes for ${recipe.name} contained one or more entries that do not exist, recipes must be created before being embedded"))
        else Future.successful()
      }
    }

    val recipePromise = Promise[Recipe]()
    Future.sequence(checkedEmbeddedRecipesFutures).onComplete {
      case Success(_) => recipePromise.completeWith(persistRecipe)
      case Failure(t) => recipePromise.failure(t)
    }

    recipePromise.future
  }
}
