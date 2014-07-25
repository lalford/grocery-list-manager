package controllers

import play.api.mvc.{SimpleResult, Action, Controller}
import play.api.libs.json.{JsError, Json}
import services.{RecipeService, GroceryListService}
import scala.concurrent.{Promise, Future}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.util.{Failure, Success}

class GroceryListController(
  groceryListService: GroceryListService,
  recipeService: RecipeService ) extends Controller with TemplateHelpers with RequestHelpers {

  import models._
  import models.JsonFormats._

  def viewGroceryLists = ActionHelper.async { implicit requestContext =>
    groceryListService.findGroceryLists map { allGroceryLists =>
      val sortedGroceryLists = allGroceryLists sortWith(_.name.toLowerCase < _.name.toLowerCase)
      Ok(views.html.groceryLists(sortedGroceryLists))
    }
  }

  def viewGroceryList(name: String) = ActionHelper.async { implicit requestContext =>
    groceryListService.findGroceryList(name) map {
      case None => NotFound
      case Some(groceryList) => Ok(views.html.groceryList(groceryList))
    }
  }

  def newGroceryList = ActionHelper { implicit requestContext =>
    Ok(views.html.newGroceryList(emptyGroceryListForm))
  }

  def findGroceryLists = Action.async {
    groceryListService.findGroceryLists map { allGroceryLists => Ok(Json.toJson(allGroceryLists)) }
  }
  
  def findGroceryList(name: String) = Action.async {
    groceryListService.findGroceryList(name) map {
      case None => NotFound
      case Some(groceryList) => Ok(Json.toJson(groceryList))
    }
  }

  def createEmptyGroceryList = Action.async { implicit request =>
    emptyGroceryListForm.bindFromRequest.fold(
      formWithErrors => Future.successful(Redirect(routes.GroceryListController.newGroceryList).flashing("error" -> formErrorsFlashing(formWithErrors))),
      emptyGroceryListName => {
        val result = Promise[SimpleResult]()
        groceryListService.insertGroceryList(GroceryList(emptyGroceryListName)).onComplete {
          case Success(groceryList) =>
            result.success(Redirect(routes.GroceryListController.viewGroceryList(groceryList.name)).flashing("success" -> s"Grocery List Created - $emptyGroceryListName"))
          case Failure(e) if e.isInstanceOf[IllegalArgumentException] =>
            result.success(Redirect(routes.GroceryListController.newGroceryList).flashing("error" -> s"Failed - ${e.getMessage}"))
          case Failure(e) =>
            result.success(InternalServerError)
        }
        result.future
      }
    )
  }

  def createGroceryList = Action.async(parse.json) { request =>
    request.body.validate[GroceryList].fold (
      jsErrors => Future.successful(BadRequest("json is not valid as a grocery list:"+ JsError.toFlatJson(jsErrors))),
      groceryList => {
        val result = Promise[SimpleResult]()
        groceryListService.insertGroceryList(groceryList).onComplete {
          case Success(_) => result.success(Created)
          case Failure(e) if e.isInstanceOf[IllegalArgumentException] => result.success(BadRequest(e.getMessage))
          case Failure(e) => result.success(InternalServerError)
        }
        result.future
      }
    )
  }

  def addRecipeServings(name: String) = Action.async { implicit request =>
    def addToList(addRecipeServingData: AddRecipeServing) = {
      val redirectUrl = addRecipeServingData.redirectUrl
      val activeGroceryList = addRecipeServingData.activeGroceryList
      val recipeName = addRecipeServingData.recipeServing.name
      val desiredServings = addRecipeServingData.recipeServing.desiredServings

      val updatedGroceryListPromise = Promise[GroceryList]()
      val resultPromise = Promise[SimpleResult]()

      groceryListService.findGroceryList(activeGroceryList) onComplete {
        case Success(groceryListOpt) =>
          if (groceryListOpt.isEmpty)
            updatedGroceryListPromise.failure(new IllegalStateException(s"could not find active grocery list $activeGroceryList"))
          else {
            val newRecipeServing = RecipeServing(recipeName, desiredServings)
            val groceryList = groceryListOpt.get
            val newRecipeServings = newRecipeServing :: groceryList.recipeServings
            val updatedGroceryList = groceryList.copy(recipeServings = newRecipeServings)

            updatedGroceryListPromise.completeWith(groceryListService.updateGroceryList(updatedGroceryList))
          }
        case Failure(t) =>
          updatedGroceryListPromise.failure(t)
      }

      updatedGroceryListPromise.future onComplete {
        case Success(_) =>
          resultPromise.success(Redirect(redirectUrl).flashing("success" -> s"added $desiredServings servings of $recipeName to list $activeGroceryList"))
        case Failure(t) =>
          val err = s"failed to add $desiredServings servings of $recipeName to list $activeGroceryList"
          Logger.error(err, t)
          resultPromise.success(Redirect(redirectUrl).flashing("error" -> err))
      }

      resultPromise.future
    }

    addRecipeServingForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(s"add recipe servings form invalid. ${formWithErrors.errors.map(_.message).mkString}")),
      addToList
    )
  }

  def updateGroceryList = Action.async(parse.json) { request =>
    request.body.validate[GroceryList].fold(
      jsErrors => Future.successful(BadRequest("json is not valid as a grocery list:"+ JsError.toFlatJson(jsErrors))),
      groceryList => {
        groceryListService.updateGroceryList(groceryList) map { lastError =>
          Logger.debug(s"Successfully updated with LastError: $lastError")
          Ok
        }
      }
    )
  }

  def generateShoppingList(name: String) = Action.async {
    groceryListService.findGroceryList(name) flatMap {
      case None => Future.successful(NotFound)
      case Some(groceryList) => {
        val allIngredients = Promise[List[FoodIngredient]]()
        val shoppingList = Promise[Map[StoreSection, Map[Food, List[QuantityUnit]]]]()

        allIngredients completeWith {
          findRecipesAndServings(groceryList) flatMap { recipesAndServings =>
            findIngredientsFromRecipes(recipesAndServings) map (_ ::: groceryList.miscellaneous)
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

  def makeActiveGroceryList(name: String, redirectUrl: String) = Action {
    Redirect(redirectUrl).withSession(ActionConstants.activeGroceryListKey -> name)
  }

  private def findRecipesAndServings(groceryList: GroceryList): Future[List[(Recipe, Double)]] = {
    Future.traverse(groceryList.recipeServings) { recipeServing =>
      recipeService.findRecipe(recipeServing.name).map { recipeOpt =>
        val recipe = recipeOpt.getOrElse(throw new IllegalStateException(s"could not find recipe ${recipeServing.name}, which is still referenced in grocery list ${groceryList.name}"))
        recipe -> recipeServing.desiredServings
      }
    }
  }

  private def findIngredientsFromRecipes(recipesAndServings: List[(Recipe, Double)]): Future[List[FoodIngredient]] = {
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
        val futureRecipeOpt = recipeService.findRecipe(rs.name)
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
}

object GroceryListController extends GroceryListController(new GroceryListService, new RecipeService)