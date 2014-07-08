package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def javascriptRoutes = Action { implicit request =>
    import routes.javascript._
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        GroceryListController.viewGroceryList,
        GroceryListController.findGroceryLists,
        GroceryListController.findGroceryList,
        GroceryListController.updateGroceryList,
        GroceryListController.generateShoppingList,
        RecipeController.viewRecipes,
        RecipeController.findRecipes,
        RecipeController.findRecipe,
        RecipeController.createRecipe,
        RecipeController.updateRecipe
      )
    ).as("text/javascript")
  }

  // TODO - change/remove below cruft

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

}