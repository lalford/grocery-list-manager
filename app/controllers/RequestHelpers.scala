package controllers

import models._
import play.api.data.Form
import play.api.data.Forms._

trait RequestHelpers {
  val emptyGroceryListForm = Form(
    single("name" -> nonEmptyText)
  )

  val addRecipeServingForm = Form(
    mapping(
      "redirectUrl" -> nonEmptyText,
      "activeGroceryList" -> nonEmptyText,
      "recipeServing" -> mapping(
        "name" -> nonEmptyText,
        "desiredServings" -> bigDecimal
      )(recipeServingApply)(recipeServingUnapply)
    )(AddRecipeServing.apply)(AddRecipeServing.unapply)
  )

  def recipeServingApply(name: RecipeName, desiredServings: BigDecimal) = RecipeServing(name, desiredServings.toDouble)
  def recipeServingUnapply(recipeServing: RecipeServing) = Some( (recipeServing.name, BigDecimal(recipeServing.desiredServings)) )
}

case class AddRecipeServing(redirectUrl: String, activeGroceryList: String, recipeServing: RecipeServing)
