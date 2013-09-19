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
          case RegimeRoots(Some(paye), None, None) => FrontEndRedirect.forSession(session)
          case RegimeRoots(None, Some(sa), _) => FrontEndRedirect.toBusinessTax
          case RegimeRoots(None, _, Some(vat)) => FrontEndRedirect.toBusinessTax
          case _ => Unauthorized(login.render())
        }

  })

}