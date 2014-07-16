package models

import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.bson.{BSONDateTime, BSONHandler, Macros}

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
                        recipeServings: List[RecipeServing] = List(),
                        miscellaneous: List[FoodIngredient] = List(),
                        created: Option[DateTime] = Some(DateTime.now) )

case class RecipeServing(name: RecipeName, desiredServings: Double)

case class QuantityUnit(quantity: Double, unit: String)

object JsonFormats {
  implicit val foodIngredientFormat = Json.format[FoodIngredient]
  implicit val recipeServingFormat = Json.format[RecipeServing]
  implicit val recipeFormat = Json.format[Recipe]
  implicit val groceryListFormat = Json.format[GroceryList]
  implicit val quantityUnitFormat = Json.format[QuantityUnit]
}

object BSONHandlers {
  implicit object DateHandler extends BSONHandler[BSONDateTime, DateTime]{
    def write(t: DateTime): BSONDateTime = BSONDateTime(t.toDate.getTime)
    def read(bson: BSONDateTime): DateTime = new DateTime(bson.value)
  }

  implicit val foodIngredientHandler = Macros.handler[FoodIngredient]
  implicit val recipeServingHandler = Macros.handler[RecipeServing]
  implicit val recipeHandler = Macros.handler[Recipe]
  implicit val groceryListHandler = Macros.handler[GroceryList]
  implicit val quantityUnitHandler = Macros.handler[QuantityUnit]
}