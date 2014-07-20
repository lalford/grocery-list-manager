package controllers

// This exists to strip the need for the generic typed request object from play and implicitly feeding templates with the data extracted by the request wrapper
trait TemplateHelpers {
  implicit def requestContextData[A](implicit requestContext: RequestContext[A]) = {
    RequestContextData(
      requestContext.groceryListActivators,
      requestContext.activeGroceryList,
      requestContext.navigationLinks
    )
  }
}

case class RequestContextData(groceryListActivators: List[GroceryListActivator],
                              activeGroceryList: Option[ActiveGroceryList],
                              navigationLinks: NavigationLinks)