package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import services.{GroceryListService, RecipeService}
import scala.concurrent.{Promise, Future}
import scala.util.{Failure, Success}

class RecipeController(recipeService: RecipeService, groceryListService: GroceryListService) extends Controller with TemplateHelpers with RequestHelpers {
  import models._
  import models.JsonFormats._

  def viewRecipes = ActionHelper.async { implicit requestContext =>
    recipeService.findRecipes map { allRecipes =>
      Ok(views.html.recipes(allRecipes))
    }
  }

  def viewRecipe(name: String) = ActionHelper.async { implicit requestContext =>
    recipeService.findRecipe(name) map {
      case None => NotFound
      case Some(recipe) => Ok(views.html.recipe(recipe))
    }
  }

  def findRecipes = Action.async {
    recipeService.findRecipes map { allRecipes =>
      Ok(Json.toJson(allRecipes))
    }
  }

  def findRecipe(name: String) = Action.async {
    recipeService.findRecipe(name) map {
      case None => NotFound
      case Some(recipe) => Ok(Json.toJson(recipe))
    }
  }

  def newRecipe = ActionHelper { implicit requestContext =>
    Ok(views.html.newRecipe(recipeForm))
  }

  def createFormRecipe = Action.async { implicit request =>
    recipeForm.bindFromRequest.fold(
      formWithErrors => Future.successful(Redirect(routes.RecipeController.newRecipe).flashing("error" -> formErrorsFlashing(formWithErrors))),
      boundRecipe => {
        val result = Promise[SimpleResult]()
        recipeService.insertRecipe(boundRecipe).onComplete {
          case Success(recipe) =>
            result.success(Redirect(routes.RecipeController.viewRecipe(recipe.name)).flashing("success" -> s"Recipe Created - ${recipe.name}"))
          case Failure(e) if e.isInstanceOf[IllegalArgumentException] =>
            result.success(Redirect(routes.RecipeController.newRecipe).flashing("error" -> s"Failed - ${e.getMessage}"))
          case Failure(e) =>
            result.success(InternalServerError)
        }
        result.future
      }
    )
  }

  def createRecipe = Action.async(parse.json) { request =>
    request.body.validate[Recipe].fold(
      jsErrors => Future.successful(BadRequest("json is not valid as a recipe:"+ JsError.toFlatJson(jsErrors))),
      recipe => {
        val result = Promise[SimpleResult]()
        recipeService.insertRecipe(recipe).onComplete {
          case Success(_) => result.success(Created)
          case Failure(e) if e.isInstanceOf[IllegalArgumentException] => result.success(BadRequest(e.getMessage))
          case Failure(e) => result.success(InternalServerError)
        }
        result.future
      }
    )
  }

  def editRecipe(name: String) = ActionHelper.async { implicit requestContext =>
    recipeService.findRecipe(name) map { recipe =>
      recipe.map(r => Ok(views.html.editRecipe(recipeForm.fill(r)))).getOrElse(NotFound)
    }
  }

  def updateFormRecipe = Action.async { implicit request =>
    val redirectUrl = request.getQueryString("redirectUrl")
    recipeForm.bindFromRequest.fold(
      formWithErrors => {
        val result = redirectUrl.map(Redirect(_)).getOrElse(BadRequest(s"recipe form invalid. ${formWithErrors.errors.map(_.message).mkString}"))
        Future.successful(result.flashing("error" -> formErrorsFlashing(formWithErrors)))
      },
      boundRecipe => {
        val result = Promise[SimpleResult]()
        recipeService.updateRecipe(boundRecipe).onComplete {
          case Success(recipe) =>
            result.success(Redirect(routes.RecipeController.viewRecipe(recipe.name)).flashing("success" -> s"Recipe Updated - ${recipe.name}"))
          case Failure(e) if e.isInstanceOf[IllegalArgumentException] =>
            result.success(Redirect(routes.RecipeController.editRecipe(boundRecipe.name)).flashing("error" -> s"Failed - ${e.getMessage}"))
          case Failure(e) =>
            result.success(InternalServerError)
        }
        result.future
      }
    )
  }

  def updateRecipe = Action.async(parse.json) { request =>
    request.body.validate[Recipe].fold(
      jsErrors => Future.successful(BadRequest("json is not valid as a recipe:"+ JsError.toFlatJson(jsErrors))),
      recipe => {
        recipeService.updateRecipe(recipe) map { lastError =>
          Logger.debug(s"Successfully updated with LastError: $lastError")
          Ok
        }
      }
    )
  }

  def deleteRecipe(name: String) = Action.async {
    val result = Redirect(routes.RecipeController.viewRecipes)
    val resultPromise = Promise[SimpleResult]()

    // TODO - need working strategy on the view side for multiple success flashing for all results, can't use the same key
    for {
      groceryListDeletes <- groceryListService.deleteRecipeFromGroceryLists(name)
      otherRecipeDeletes <- recipeService.deleteRecipeFromOtherRecipes(name)
      recipeDelete <- recipeService.deleteRecipe(name)
    } yield {
      val groceryListDeletesFlashing = groceryListDeletes.filter(_.isDefined).map(nameOpt => "success" -> s"Recipe Removed From Grocery List - ${nameOpt.get}")
      val otherRecipeDeletesFlashing = otherRecipeDeletes.filter(_.isDefined).map(nameOpt => "success" -> s"Recipe Removed From Ingredients - ${nameOpt.get}")
      val recipeDeleteFlashing = List("success" -> s"Recipe Removed - $name")

      val flashing = groceryListDeletesFlashing ++ otherRecipeDeletesFlashing ++ recipeDeleteFlashing
      resultPromise.success(flashing.foldLeft(result) { case (res, next) => res.flashing(next) })
    }

    resultPromise.future
  }

  def recipesAutocomplete = Action.async { request =>
    val term = request.getQueryString("term")
    recipeService.findRecipes map { recipes =>
      val matchingRecipes = term.map(t => recipes.filter(_.name.toLowerCase.contains(t.toLowerCase))).getOrElse(recipes)
      val names = matchingRecipes map { recipe =>
        JsObject(Seq(
          "label" -> JsString(recipe.name),
          "value" -> JsString(recipe.name)
        ))
      }
      Ok(Json.toJson(JsArray(names)))
    }
  }

  def foodsAutocomplete = Action.async { implicit request => foodIngredientsFieldAutocomplete { fi => fi.food } }
  def unitsAutocomplete = Action.async { implicit request => foodIngredientsFieldAutocomplete { fi => fi.unit.getOrElse("") } }
  def storeSectionAutocomplete = Action.async { implicit request => foodIngredientsFieldAutocomplete { fi => fi.storeSection.getOrElse("") } }

  private def foodIngredientsFieldAutocomplete(fieldFunc: FoodIngredient => String)(implicit request: Request[AnyContent]) = {
    val term = request.getQueryString("term")
    recipeService.findRecipes map { recipes =>
      val foodIngredientFields = recipes.flatMap(recipe => recipe.foodIngredients.map(fieldFunc)).toSet
      val matchingFields = term.map(t => foodIngredientFields.filter(f => f.nonEmpty && f.toLowerCase.contains(t.toLowerCase))).getOrElse(foodIngredientFields)
      val fields = matchingFields map { field =>
        JsObject(Seq(
          "label" -> JsString(field),
          "value" -> JsString(field)
        ))
      }
      Ok(Json.toJson(JsArray(fields.toSeq)))
    }
  }
}

object RecipeController extends RecipeController(new RecipeService, new GroceryListService)