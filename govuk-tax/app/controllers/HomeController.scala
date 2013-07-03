package controllers

import play.api.mvc.Action
import play.Logger
import microservice.domain.{ RegimeRoots, User }

class HomeController extends BaseController with ActionWrappers {

  def landing = Action {

    Logger.debug("Landing...")
    Ok(views.html.login())

  }

  def home = AuthorisedForAction {
    user =>
      request =>

        Logger.debug("Chosing home for $user")

        user.regimes match {
          case RegimeRoots(Some(payeRoot), None) => Redirect(routes.PayeController.home())
          case RegimeRoots(None, Some(saRoot)) => Redirect(routes.SaController.home())
        }

  }

}
