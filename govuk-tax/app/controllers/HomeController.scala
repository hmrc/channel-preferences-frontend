package controllers

import play.api.mvc.Action
import play.Logger
import microservice.domain.RegimeRoots
import views.html.login

class HomeController extends BaseController with ActionWrappers {

  def landing = Action {

    Logger.debug("Landing...")
    Ok(views.html.login())

  }

  def home = AuthorisedForAction {
    user =>
      request =>

        Logger.debug("Choosing home for $user")

        user.regimes match {
          case RegimeRoots(Some(paye), None) => Redirect(routes.PayeController.home())
          case RegimeRoots(None, Some(sa)) => Redirect(routes.SaController.home())
          case _ => Unauthorized(login.render())
        }

  }

}
