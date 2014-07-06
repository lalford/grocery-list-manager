package controllers

import models._
import play.api.data.Form
import play.api.data.Forms._

trait RequestHelpers {
  val emptyGroceryListForm = Form(
    single("name" -> nonEmptyText)
  )

  val recipeServingForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "desiredServings" -> bigDecimal
    )(recipeServingApply)(recipeServingUnapply)
  )

  def recipeServingApply(name: RecipeName, desiredServings: BigDecimal) = RecipeServing(name, desiredServings.toDouble)
  def recipeServingUnapply(recipeServing: RecipeServing) = Some( (recipeServing.name, BigDecimal(recipeServing.desiredServings)) )
}
