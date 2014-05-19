package controllers

import play.api.mvc.{WrappedRequest, SimpleResult, Request, ActionBuilder}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Logger

object ActionWrapper extends ActionBuilder[RequestWrapper] {
  def invokeBlock[A](request: Request[A], block: (RequestWrapper[A]) => Future[SimpleResult]) = {
    val currentUri = request.uri
    val currentQueryStr = request.rawQueryString
    val redirectUrl = s"$currentUri$currentQueryStr"

    GroceryListController.fetchGroceryLists flatMap { groceryLists =>
      val glNameUrlPairs = groceryLists map { gl =>
        val url = routes.GroceryListController.makeActiveGroceryList(gl.name, redirectUrl).url
        (gl.name, url)
      }
      block(RequestWrapper(glNameUrlPairs, request))
    }
  }
}

case class RequestWrapper[A](groceryListNameUrlPairs: List[(String, String)], request: Request[A]) extends WrappedRequest[A](request)