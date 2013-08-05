package controllers

import play.Logger
import microservice.domain.RegimeRoots
import views.html.login

class HomeController extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  def landing = UnauthorisedAction { implicit request =>

    Logger.debug("Landing...")
    Ok(login())

  }

  def home = WithSessionTimeoutValidation(AuthorisedForIdaAction() {
    user =>
      request =>

        Logger.debug("Choosing home for $user")

        user.regimes match {
          case RegimeRoots(Some(paye), None, None) => Redirect(routes.PayeController.home())
          case RegimeRoots(None, Some(sa), _) => Redirect(routes.BusinessTaxController.home())
          case RegimeRoots(None, _, Some(vat)) => Redirect(routes.BusinessTaxController.home())
          case _ => Unauthorized(login.render())
        }

  })

}
