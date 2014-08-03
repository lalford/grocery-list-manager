package controllers

import models._
import play.api.data.Form
import play.api.data.Forms._

trait RequestHelpers {
  val emptyGroceryListForm = Form(
    single("name" -> nonEmptyText)
  )

  val recipeServingMapping = mapping(
    "name" -> nonEmptyText,
    "desiredServings" -> bigDecimal.verifying(_ > 0)
  )(recipeServingApply)(recipeServingUnapply)

  def recipeServingApply(name: RecipeName, desiredServings: BigDecimal) = RecipeServing(name, desiredServings.toDouble)
  def recipeServingUnapply(recipeServing: RecipeServing) = Some( (recipeServing.name, BigDecimal(recipeServing.desiredServings)) )

  val addRecipeServingForm = Form(
    mapping(
      "redirectUrl" -> nonEmptyText,
      "activeGroceryList" -> nonEmptyText,
      "recipeServing" -> recipeServingMapping
    )(AddRecipeServing.apply)(AddRecipeServing.unapply)
  )

  val foodIngredientMapping = mapping(
    "food" -> nonEmptyText,
    "quantity" -> bigDecimal.verifying(_ > 0),
    "unit" -> optional(nonEmptyText),
    "storeSection" -> optional(nonEmptyText)
  )(foodIngredientApply)(foodIngredientUnapply)

  def foodIngredientApply(food: Food, quantity: BigDecimal, unit: Option[String], storeSection: Option[StoreSection]) = FoodIngredient(food, quantity.toDouble, unit, storeSection)
  def foodIngredientUnapply(fi: FoodIngredient) = Some( (fi.food, BigDecimal(fi.quantity), fi.unit, fi.storeSection) )

  val recipeForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "servings" -> bigDecimal.verifying(_ > 0),
      "foodIngredients" -> list(foodIngredientMapping),
      "recipeIngredients" -> list(recipeServingMapping),
      "directions" -> optional(text(maxLength = 10000)),
      "tags" -> list(nonEmptyText)
    )(recipeApply)(recipeUnapply)
  )

  def recipeApply(name: RecipeName,
                  servings: BigDecimal,
                  foodIngredients: List[FoodIngredient] = List(),
                  recipeIngredients: List[RecipeServing] = List(),
                  directions: Option[String] = None,
                  tags: List[String] = List()) = {
    Recipe(name, servings.toDouble, foodIngredients, recipeIngredients, directions, tags)
  }

  def recipeUnapply(r: Recipe) = Some((r.name, BigDecimal(r.servings), r.foodIngredients, r.recipeIngredients, r.directions, r.tags))

  def formErrorsFlashing[T](formWithErrors: Form[T]) = {
    formWithErrors.errors.map(e => s"${e.key} - ${e.message}").mkString("\n")
  }
}

case class AddRecipeServing(redirectUrl: String, activeGroceryList: String, recipeServing: RecipeServing)
