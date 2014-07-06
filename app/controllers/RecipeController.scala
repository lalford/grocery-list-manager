package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scala.concurrent.Future
import anorm._
import play.api.db.DB
import org.joda.time.DateTime
import java.util.Date

// Reactive Mongo imports
import reactivemongo.api._

// Reactive Mongo plugin, including the JSON-specialized collection
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection

object RecipeController extends Controller with MongoController with TemplateData with RequestHelpers {
  def collection: JSONCollection = db.collection[JSONCollection]("recipes")

  import models._
  import models.JsonFormats._

  def viewRecipes = ActionWrapper.async { implicit requestWrapper =>
    fetchRecipes map { allRecipes =>
      val recipesWithForm = allRecipes.map(r => r -> recipeServingForm.fill(RecipeServing(r.name, 0)))
      Ok(views.html.recipes(recipesWithForm))
    }
  }

  def findRecipes = Action.async {
    val futureAllRecipes = fetchRecipes
    futureAllRecipes map { allRecipes => Ok(Json.toJson(allRecipes)) }
  }

  def findRecipe(name: String) = Action.async {
    fetchRecipeByName(name) map {
      case None => NotFound
      case Some(recipe) => Ok(Json.toJson(recipe))
    }
  }

  def createRecipe = Action.async(parse.json) { request =>
    request.body.validate[Recipe].map { recipe =>
      fetchRecipeByName(recipe.name).flatMap {
        case None => {
          collection.insert(recipe).map { lastError =>
            Logger.debug(s"Successfully inserted with LastError: $lastError")
            Created
          }
        }
        case Some(r) => Future.successful(BadRequest(s"a recipe already exists named [${r.name}]"))
      }
    }.getOrElse(Future.successful(BadRequest("json is not valid as a recipe")))
  }

  def updateRecipe = Action.async(parse.json) { request =>
    request.body.validate[Recipe].map { recipe =>
      collection
        .update(Json.obj("name" -> recipe.name), recipe)
        .map { lastError =>
          Logger.debug(s"Successfully updated with LastError: $lastError")
          Ok
        }
    }.getOrElse(Future.successful(BadRequest("json is not valid as a recipe")))
  }

  private def fetchRecipes = {
    val cursor = collection
      .find(Json.obj())
      .sort(Json.obj("name" -> 1))
      .cursor[Recipe]
    cursor.collect[List]()
  }

  def fetchRecipeByName(name: String) = {
    val cursor = collection.
      find(Json.obj("name" -> name)).
      cursor[Recipe]
    val futureRecipes = cursor.collect[List]()

    futureRecipes flatMap { recipes =>
      if (recipes.length > 1) Future.failed(new IllegalStateException(s"found ${recipes.length} recipes with name $name, there can be only one"))
      else Future.successful(recipes.headOption)
    }
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
      fetchRecipeByName(recipe.name).flatMap {
        case None => {
          collection.insert(recipe).map { lastError =>
            Logger.debug(s"Successfully inserted with LastError: $lastError")
            Created
          }
        }
        case Some(r) => Future.successful(BadRequest(s"a recipe already exists named [${r.name}]"))
      }
    }

    Future(Ok(extractedRecipes.mkString("\n\n")))
  }
}