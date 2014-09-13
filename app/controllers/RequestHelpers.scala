package controllers

import models._
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import scala.util.control.Exception.allCatch

trait RequestHelpers {
  val emptyGroceryListForm = Form(
    single("name" -> nonEmptyText)
  )

  val recipeServingMapping = mapping(
    "name" -> nonEmptyText,
    "desiredServings" -> nonEmptyText.verifying(RequestHelpers.evaluate(_).isDefined)
  )(recipeServingApply)(recipeServingUnapply)

  def recipeServingApply(name: RecipeName, desiredServings: String) = RecipeServing(name, RequestHelpers.evaluate(desiredServings).get)
  def recipeServingUnapply(recipeServing: RecipeServing) = Option( (recipeServing.name, recipeServing.desiredServings.toString) )

  val addRecipeServingForm = Form(
    mapping(
      "redirectUrl" -> nonEmptyText,
      "activeGroceryList" -> nonEmptyText,
      "recipeServing" -> recipeServingMapping
    )(AddRecipeServing.apply)(AddRecipeServing.unapply)
  )

  val foodIngredientMapping = mapping(
    "food" -> nonEmptyText,
    "quantity" -> nonEmptyText.verifying(RequestHelpers.evaluate(_).isDefined),
    "unit" -> optional(nonEmptyText),
    "storeSection" -> optional(nonEmptyText)
  )(foodIngredientApply)(foodIngredientUnapply)

  def foodIngredientApply(food: Food, quantity: String, unit: Option[String], storeSection: Option[StoreSection]) = FoodIngredient(food, RequestHelpers.evaluate(quantity).get, unit, storeSection)
  def foodIngredientUnapply(fi: FoodIngredient) = Some( (fi.food, fi.quantity.toString, fi.unit, fi.storeSection) )

  val recipeForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "servings" -> nonEmptyText.verifying(RequestHelpers.evaluate(_).isDefined),
      "foodIngredients" -> list(foodIngredientMapping),
      "recipeIngredients" -> list(recipeServingMapping),
      "directions" -> optional(text(maxLength = 10000)),
      "tags" -> list(nonEmptyText)
    )(recipeApply)(recipeUnapply)
  )

  def recipeApply(name: RecipeName,
                  servings: String,
                  foodIngredients: List[FoodIngredient] = List(),
                  recipeIngredients: List[RecipeServing] = List(),
                  directions: Option[String] = None,
                  tags: List[String] = List()) = {
    Recipe(name, RequestHelpers.evaluate(servings).get, foodIngredients, recipeIngredients, directions, tags)
  }

  def recipeUnapply(r: Recipe) = Some((r.name, r.servings.toString, r.foodIngredients, r.recipeIngredients, r.directions, r.tags))

  val groceryListForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "recipeServings" -> list(recipeServingMapping),
      "miscellaneous" -> list(foodIngredientMapping)
    )(groceryListApply)(groceryListUnapply)
  )

  def groceryListApply(name: String,
                       recipeServings: List[RecipeServing] = List(),
                       miscellaneous: List[FoodIngredient] = List()) = {
    GroceryList(name, recipeServings, miscellaneous)
  }

  def groceryListUnapply(gl: GroceryList) = Some((gl.name, gl.recipeServings, gl.miscellaneous))

  def formErrorsFlashing[T](formWithErrors: Form[T]) = {
    formWithErrors.errors.map(e => s"${e.key} - ${e.message}").mkString("\n")
  }
}

object RequestHelpers {
  import javax.script.ScriptEngineManager

  val mgr = new ScriptEngineManager()
  val engine = mgr.getEngineByName("JavaScript")

  def evaluate(expression: String): Option[Double] = allCatch.either(engine.eval(expression)).fold(bad, good)

  private def bad(t: Throwable): Option[Double] = {
    Logger.error("failed to evaluate expression", t)
    None
  }

  // not ideal, but the expression evaluator is nice. It's a javascript engine so the result comes back as an object, but underneath it can be cast as a double
  private def good(bd: Object): Option[Double] = Option(bd.asInstanceOf[Double])
}

case class AddRecipeServing(redirectUrl: String, activeGroceryList: String, recipeServing: RecipeServing)
