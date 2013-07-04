package controllers

import play.api.mvc.Action
import play.Logger
import microservice.domain.User

class HomeController extends BaseController with ActionWrappers {

  def landing = Action {

    Logger.debug("Landing...")
    Ok(views.html.login())

  }

  def home = AuthorisedForAction {
    user =>
      request =>

        Logger.debug("Choosing home for $user")

        user match {
          case User("/auth/oid/jdensmore", _, _, _) => Redirect(routes.PayeController.home())
          case User("/auth/oid/newjdensmore", _, _, _) => Redirect(routes.PayeController.home())
          case User("/auth/oid/gfisher", _, _, _) => Redirect(routes.SaController.home())
        }

  }

}
