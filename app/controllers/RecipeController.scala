package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import services.RecipeService
import scala.concurrent.{Promise, Future}
import anorm._
import play.api.db.DB
import org.joda.time.DateTime
import java.util.Date

import scala.util.{Failure, Success}

class RecipeController(recipeService: RecipeService) extends Controller with TemplateHelpers with RequestHelpers {
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
      formWithErrors => Future.successful(BadRequest("recipe name cannot be empty")),
      recipe => Future.successful(NotImplemented)
//      {
//        val result = Promise[SimpleResult]()
//        groceryListService.insertGroceryList(GroceryList(emptyGroceryListName)).onComplete {
//          case Success(groceryList) =>
//            result.success(Redirect(routes.GroceryListController.viewGroceryList(groceryList.name)).flashing("result" -> s"Grocery List Created - $emptyGroceryListName"))
//          case Failure(e) if e.isInstanceOf[IllegalArgumentException] =>
//            result.success(Redirect(routes.GroceryListController.newGroceryList).flashing("result" -> s"Failed - ${e.getMessage}"))
//          case Failure(e) =>
//            result.success(InternalServerError)
//        }
//        result.future
//      }
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

  // TODO - migration can be removed once everything is settled
  val findBasicRecipeData = SQL(
    """
      select name, servings, directions, created_at created
      from recipes
      order by name;
    """)

  val findRecipeIngredients = SQL(
    """
      select f.name food, i.quantity, i.unit_name, ss.name store_section
      from foods f
      join store_sections ss on (ss.id = f.store_section_id)
      join ingredients i on (i.food_id = f.id)
      join recipes r on (r.id = i.recipe_id)
      where r.name = {recipeName};
    """)

  // TODO - correct execution context management for blocking operations
  import play.api.libs.concurrent.Execution.Implicits.defaultContext
  import play.api.Play.current
  def migrateData = Action.async {
    val extractedRecipes = DB.withConnection { implicit c =>
      val recipes = findBasicRecipeData().map(row => {
        Recipe(
          name = row[String]("name"),
          servings = row[Int]("servings").toDouble,
          foodIngredients = List(),
          recipeIngredients = List(),
          directions = row[Option[String]]("directions"),
          tags = List("recipe"),
          created = Option(new DateTime(row[Date]("created")))
        )
      }).toList

      recipes.map(recipe => {
        val foodIngredients = findRecipeIngredients.on("recipeName" -> recipe.name)().map(row => {
          FoodIngredient(
            food = row[String]("food"),
            quantity = row[Option[Double]]("quantity").getOrElse(0),
            unit = row[Option[String]]("unit_name"),
            storeSection = row[Option[String]]("store_section")
          )
        }).toList

        Recipe(
          recipe.name,
          recipe.servings,
          foodIngredients,
          List(), // nested recipes previously unsupported
          recipe.directions,
          recipe.tags,
          recipe.created
        )
      })
    }

    extractedRecipes.foreach { recipe =>
      val result = Promise[SimpleResult]()
      recipeService.insertRecipe(recipe).onComplete {
        case Success(_) => result.success(Created)
        case Failure(e) if e.isInstanceOf[IllegalArgumentException] => result.success(BadRequest(e.getMessage))
        case Failure(e) => result.success(InternalServerError)
      }
      result.future
    }

    Future(Ok(extractedRecipes.mkString("\n\n")))
  }
}

object RecipeController extends RecipeController(new RecipeService)