package controllers

import models.GroceryList
import play.api.mvc.{WrappedRequest, ActionBuilder, SimpleResult, Request}
import services.GroceryListService
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object ActionHelper extends ActionBuilder[RequestContext] {
  val groceryListService = new GroceryListService

  def invokeBlock[A](request: Request[A], block: (RequestContext[A]) => Future[SimpleResult]) = {
    val currentUri = request.uri
    val currentQueryStr = request.rawQueryString
    val redirectUrl = s"$currentUri$currentQueryStr"

    groceryListService.findGroceryLists flatMap { groceryLists =>
      val sorted = groceryLists.sortWith(_.name.toLowerCase < _.name.toLowerCase)
      val groceryListActivators = sorted.map(gl => GroceryListActivator(gl.name, routes.GroceryListController.makeActiveGroceryList(gl.name, redirectUrl).url))

      val activeGroceryList = for {
        name <- request.session.get(ActionConstants.activeGroceryListKey)
        gl <- groceryLists.find(_.name == name)
      } yield ActiveGroceryList(gl, routes.GroceryListController.viewGroceryList(name).url)

      val navigationLinks = NavigationLinks(
        lastUrl = redirectUrl,
        newGroceryListUrl = routes.GroceryListController.newGroceryList.url,
        viewGroceryListsUrl = routes.GroceryListController.viewGroceryLists.url,
        viewRecipesUrl = routes.RecipeController.viewRecipes.url
      )

      val requestContext = RequestContext(
        request,
        groceryListActivators,
        activeGroceryList,
        navigationLinks
      )

      block(requestContext)
    }
  }
}

object ActionConstants {
  val activeGroceryListKey = "activeGroceryList"
}

trait TemplateData {
  implicit def requestContext
}

case class GroceryListActivator(name: String, activatorUrl: String)

case class ActiveGroceryList(groceryList: GroceryList, groceryListUrl: String)

case class NavigationLinks(lastUrl: String,
                           newGroceryListUrl: String,
                           viewGroceryListsUrl: String,
                           viewRecipesUrl: String)

case class RequestContext[A](request: Request[A],
                             groceryListActivators: List[GroceryListActivator],
                             activeGroceryList: Option[ActiveGroceryList],
                             navigationLinks: NavigationLinks) extends WrappedRequest[A](request)