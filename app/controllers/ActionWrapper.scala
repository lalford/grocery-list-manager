package controllers

import play.api.mvc.{WrappedRequest, SimpleResult, Request, ActionBuilder}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object ActionWrapper extends ActionBuilder[RequestWrapper] {
  def invokeBlock[A](request: Request[A], block: (RequestWrapper[A]) => Future[SimpleResult]) = {
    GroceryListController.fetchGroceryLists flatMap { groceryLists =>
      val glNameUrlPairs = groceryLists map { gl =>
        val url = routes.GroceryListController.viewGroceryList(gl.name).url
        (gl.name, url)
      }
      block(RequestWrapper(glNameUrlPairs, request))
    }
  }
}

case class RequestWrapper[A](groceryListNameUrlPairs: List[(String, String)], request: Request[A]) extends WrappedRequest[A](request)