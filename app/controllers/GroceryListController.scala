package controllers

import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.json.Json
import scala.concurrent.{Promise, Future}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object GroceryListController extends Controller with MongoController {
  def collection: JSONCollection = db.collection[JSONCollection]("grocery_lists")

  import models._
  import models.JsonFormats._

  def findGroceryLists = Action.async {
    val cursor = collection
      .find(Json.obj())
      .sort(Json.obj("name" -> -1))
      .cursor[GroceryList]
    val futureAllGroceryLists = cursor.collect[List]()

    futureAllGroceryLists map { allGroceryLists => Ok(Json.toJson(allGroceryLists)) }
  }
  
  def findGroceryList(name: String) = Action.async {
    findGroceryListByName(name) map {
      case None => NotFound
      case Some(groceryList) => Ok(Json.toJson(groceryList))
    }
  }

  def createGroceryList = Action.async(parse.json) { request =>
    request.body.validate[GroceryList].map { groceryList =>
      findGroceryListByName(groceryList.name).flatMap {
        case None => {
          collection.insert(groceryList).map { lastError =>
            Logger.debug(s"Successfully inserted with LastError: $lastError")
            Created
          }
        }
        case Some(l) => Future.successful(BadRequest(s"a grocery list already exists named [${l.name}]"))
      }
    }.getOrElse(Future.successful(BadRequest("json is not valid as a grocery list")))
  }

  def updateGroceryList = Action.async(parse.json) { request =>
    request.body.validate[GroceryList].map { groceryList =>
      collection
        .update(Json.obj("name" -> groceryList.name), groceryList)
        .map { lastError =>
        Logger.debug(s"Successfully updated with LastError: $lastError")
        Ok
      }
    }.getOrElse(Future.successful(BadRequest("json is not valid as a grocery list")))
  }

  def generateShoppingList(name: String) = Action.async {
    findGroceryListByName(name) flatMap {
      case None => Future(NotFound)
      case Some(groceryList) => {
        val allIngredients = Promise[List[FoodIngredient]]()
        val shoppingList = Promise[Map[StoreSection, Map[Food, List[QuantityUnit]]]]()

        allIngredients completeWith {
          fetchRecipesAndServings(groceryList) flatMap { recipesAndServings =>
            fetchIngredientsFromRecipes(recipesAndServings) map (_ ::: groceryList.miscellaneous)
          }
        }

        shoppingList completeWith {
          allIngredients.future map { foodIngredients =>
            val allIngredientsByStoreSection = foodIngredients.groupBy(_.storeSection.getOrElse("unknown"))
            val allIngredientsByStoreSectionAndFood = allIngredientsByStoreSection.mapValues(_.groupBy(_.food))

            allIngredientsByStoreSectionAndFood.mapValues { ingredientsByFood =>
              ingredientsByFood.mapValues { ingredients =>
                ingredients.groupBy(_.unit.getOrElse("")).map { case (unit, ingredientsByUnit) =>
                  QuantityUnit(ingredientsByUnit.map(_.quantity).sum, unit)
                }.toList
              }
            }
          }
        }

        shoppingList.future map (sl => Ok(Json.toJson(sl)))
      }
    }
  }

  private def fetchRecipesAndServings(groceryList: GroceryList): Future[List[(Recipe, Double)]] = {
    Future.sequence {
      groceryList.recipeServings.map { rs =>
        val futureRecipeOpt = RecipeController.fetchRecipeByName(rs.name)
        futureRecipeOpt.map { recipeOpt => (recipeOpt, rs.desiredServings) }
      }
    }.map { recipesAndServings =>
      recipesAndServings
        .filter { case (recipeOpt, _) => recipeOpt.nonEmpty }
        .map { case (recipeOpt, desiredServings) => (recipeOpt.get, desiredServings) }
    }
  }

  private def fetchIngredientsFromRecipes(recipesAndServings: List[(Recipe, Double)]): Future[List[FoodIngredient]] = {
    Future.traverse(recipesAndServings) { case (recipe, desiredServings) =>
      val desiredServingsRatio = desiredServings / recipe.servings
      val foodIngredients = recipe.foodIngredients.map(quantityAdjusted(_, desiredServingsRatio))
      accumulateFoodIngredients(List(), recipe.recipeIngredients, desiredServingsRatio).map(_ ::: foodIngredients)
    }.map(_.flatten)
  }

  private def quantityAdjusted(foodIngredient: FoodIngredient, desiredServingRatio: Double) = {
    FoodIngredient(
      foodIngredient.food,
      foodIngredient.quantity * desiredServingRatio,
      foodIngredient.unit,
      foodIngredient.storeSection
    )
  }

  private def accumulateFoodIngredients(accumulated: List[FoodIngredient], recipeIngredients: List[RecipeServing], desiredServingRatio: Double): Future[List[FoodIngredient]] = {
    recipeIngredients match {
      case Nil => Future(accumulated)
      case rs :: remainingRecipeIngredients =>
        val futureRecipeOpt = RecipeController.fetchRecipeByName(rs.name)
        futureRecipeOpt flatMap {
          case None => accumulateFoodIngredients(accumulated, remainingRecipeIngredients, desiredServingRatio)
          case Some(recipe) =>
            val nestedDesiredServingsRatio = desiredServingRatio * rs.desiredServings / recipe.servings
            val foodIngredients = recipe.foodIngredients.map(quantityAdjusted(_, nestedDesiredServingsRatio))
            accumulateFoodIngredients(List(), recipe.recipeIngredients, nestedDesiredServingsRatio).flatMap { ingredients =>
              accumulateFoodIngredients(accumulated, remainingRecipeIngredients, desiredServingRatio).map(_ ::: foodIngredients)
            }
        }
    }
  }

  private def findGroceryListByName(name: String): Future[Option[GroceryList]] = {
    val cursor = collection.
      find(Json.obj("name" -> name)).
      cursor[GroceryList]
    val futureGroceryLists = cursor.collect[List]()

    futureGroceryLists flatMap { groceryLists =>
      if (groceryLists.length > 1) Future.failed(new IllegalStateException(s"found ${groceryLists.length} grocery lists with name $name, there can be only one"))
      else Future.successful(groceryLists.headOption)
    }
  }
}
