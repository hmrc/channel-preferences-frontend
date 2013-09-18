package controllers.common

import play.Logger
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import views.html._
import controllers._

class HomeController extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  def landing = UnauthorisedAction { implicit request =>
    Redirect(routes.LoginController.login()).withNewSession
  }

  def home = WithSessionTimeoutValidation(AuthorisedForIdaAction() {
    user =>
      implicit request =>

        Logger.debug("Choosing home for $user")

        user.regimes match {
          case RegimeRoots(Some(paye), None, None) => if (session.data.contains("register agent")) RedirectUtils.toAgent else RedirectUtils.toPaye
          case RegimeRoots(None, Some(sa), _) => RedirectUtils.toBusinessTax
          case RegimeRoots(None, _, Some(vat)) => RedirectUtils.toBusinessTax
          case _ => Unauthorized(login.render())
        }

  })

}