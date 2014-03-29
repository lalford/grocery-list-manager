package models

import org.joda.time.DateTime

case class Recipe( name: RecipeName,
                   servings: Double,
                   foodIngredients: List[FoodIngredient] = List(),
                   recipeIngredients: List[RecipeServing] = List(),
                   directions: Option[String] = None,
                   tags: List[String] = List(),
                   created: Option[DateTime] = Some(DateTime.now) )

case class FoodIngredient( food: String,
                           quantity: Double,
                           unit: Option[String] = None,
                           storeSection: Option[String] = None )

case class GroceryList( name: String,
                        recipeServings: List[RecipeServing],
                        miscellaneous: List[FoodIngredient],
                        created: Option[DateTime] = Some(DateTime.now) )

case class RecipeServing(name: RecipeName, desiredServings: Double)

case class QuantityUnit(quantity: Double, unit: String)

object JsonFormats {
  import play.api.libs.json.Json
  import play.api.data._
  import play.api.data.Forms._

  implicit val foodIngredientFormat = Json.format[FoodIngredient]
  implicit val recipeServingFormat = Json.format[RecipeServing]
  implicit val recipeFormat = Json.format[Recipe]
  implicit val groceryListFormat = Json.format[GroceryList]
  implicit val quantityUnitFormat = Json.format[QuantityUnit]
}
