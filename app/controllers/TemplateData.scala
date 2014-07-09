package controllers

trait TemplateData {
  val activeGroceryListKey = "activeGroceryList"

  implicit def navBarData[A](implicit requestWrapper: RequestWrapper[A]) = {
    val activeGroceryList = requestWrapper.session.get(activeGroceryListKey)
    val activeGroceryListUrl = activeGroceryList.map(name => routes.GroceryListController.viewGroceryList(name).url)
    val newGroceryListUrl = routes.GroceryListController.newGroceryList.url
    val viewGroceryListsUrl = routes.GroceryListController.viewGroceryLists.url
    val viewRecipesUrl = routes.RecipeController.viewRecipes.url

    NavBarData(
      requestWrapper.path,
      requestWrapper.groceryListNameUrlPairs,
      activeGroceryList,
      activeGroceryListUrl,
      newGroceryListUrl,
      viewGroceryListsUrl,
      viewRecipesUrl
    )
  }
}

case class NavBarData( lastUrl: String,
                       groceryListNameUrlPairs: List[(String, String)],
                       activeGroceryListName: Option[String],
                       activeGroceryListUrl: Option[String],
                       newGroceryListUrl: String,
                       viewGroceryListsUrl: String,
                       viewRecipesUrl: String )