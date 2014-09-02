package controllers

import controllers.ActionConstants.RecipePlusOneEmbedLevel
import models.GroceryList
import play.api.mvc._
import services.{RecipeService, GroceryListService}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object ActionHelper extends ActionBuilder[RequestContext] {
  val groceryListService = new GroceryListService
  val recipeService = new RecipeService

  def invokeBlock[A](request: Request[A], block: (RequestContext[A]) => Future[SimpleResult]) = {
    val activeGroceryListNameOpt = request.session.get(ActionConstants.activeGroceryListKey)
    val currentUri = request.uri
    val currentQueryStr = request.rawQueryString
    val redirectUrl = s"$currentUri$currentQueryStr"

    val requestContextFuture = for {
      groceryLists <- groceryListService.findGroceryLists
      groceryListActivators <- getGroceryListActivators(groceryLists, redirectUrl)
      activeGroceryList <- getActiveGroceryList(groceryLists, activeGroceryListNameOpt)
      navigationLinks <- getNavigationLinks(redirectUrl)
    } yield RequestContext(
      request,
      groceryListActivators,
      activeGroceryList,
      navigationLinks
    )

    requestContextFuture flatMap block
  }

  private def getGroceryListActivators(groceryLists: List[GroceryList], redirectUrl: String): Future[List[GroceryListActivator]] = Future {
    val sorted = groceryLists.sortWith(_.name.toLowerCase < _.name.toLowerCase)
    val groceryListActivators = sorted.map(gl => GroceryListActivator(gl.name, routes.GroceryListController.makeActiveGroceryList(gl.name, redirectUrl).url))
    groceryListActivators
  }

  private def getNavigationLinks(redirectUrl: String): Future[NavigationLinks] = Future {
    NavigationLinks(
      lastUrl = redirectUrl,
      newGroceryListUrl = routes.GroceryListController.newGroceryList.url,
      viewGroceryListsUrl = routes.GroceryListController.viewGroceryLists.url,
      newRecipeUrl = routes.RecipeController.newRecipe.url,
      viewRecipesUrl = routes.RecipeController.viewRecipes.url
    )
  }

  private def getActiveGroceryList(groceryLists: List[GroceryList], activeGroceryListNameOpt: Option[String]): Future[Option[ActiveGroceryList]] = {
    val groceryListOptFuture: Future[Option[GroceryList]] = Future {
      for {
        name <- activeGroceryListNameOpt
        gl <- groceryLists.find(_.name == name)
      } yield gl
    }

    val activeGroceryListOpt = for {
      groceryListOpt <- groceryListOptFuture
      recipeNamesPlus <- getRecipeNamesPlusOneEmbedLevel(groceryListOpt)
    } yield {
      groceryListOpt match {
        case Some(gl) => Some(ActiveGroceryList(gl, routes.GroceryListController.viewGroceryList(gl.name).url, routes.GroceryListController.viewShoppingList(gl.name).url, recipeNamesPlus))
        case _ => None
      }
    }

    activeGroceryListOpt
  }

  private def getRecipeNamesPlusOneEmbedLevel(groceryListOpt: Option[GroceryList]): Future[List[RecipePlusOneEmbedLevel]] = groceryListOpt match {
    case Some(groceryList) =>
      Future.traverse(groceryList.recipeServings) { recipeServing =>
        recipeService.findRecipe(recipeServing.name) map {
          case recipeOpt if recipeOpt.isDefined => recipeOpt.get.name -> recipeOpt.get.recipeIngredients.map(_.name)
          case _ => "" -> List()
        }
      }
    case _ =>
      Future.successful(List())
  }
}

object ActionConstants {
  type RecipePlusOneEmbedLevel = (String, List[String])
  val activeGroceryListKey = "activeGroceryList"
}

case class GroceryListActivator(name: String, activatorUrl: String)

case class ActiveGroceryList(groceryList: GroceryList, groceryListUrl: String, shoppingListUrl: String, recipeNamesPlusOneEmbedLevel: List[RecipePlusOneEmbedLevel])

case class NavigationLinks(lastUrl: String,
                           newGroceryListUrl: String,
                           viewGroceryListsUrl: String,
                           newRecipeUrl: String,
                           viewRecipesUrl: String)

case class RequestContext[A](request: Request[A],
                             groceryListActivators: List[GroceryListActivator],
                             activeGroceryList: Option[ActiveGroceryList],
                             navigationLinks: NavigationLinks) extends WrappedRequest[A](request)