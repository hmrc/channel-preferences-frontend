package controllers.common

import play.Logger
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import views.html._

class HomeController extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  def landing = UnauthorisedAction { implicit request =>
    Redirect(routes.LoginController.login()).withNewSession
  }

  def home = WithSessionTimeoutValidation(AuthorisedForIdaAction() {
    user =>
      implicit request =>

        Logger.debug("Choosing home for $user")

        user.regimes match {
          case RegimeRoots(Some(paye), None, None, None) => FrontEndRedirect.forSession(session)
          case RegimeRoots(None, Some(sa), _, _) => FrontEndRedirect.toBusinessTax
          case RegimeRoots(None, _, Some(vat), _) => FrontEndRedirect.toBusinessTax
          case RegimeRoots(None, _, _, Some(epaye)) => FrontEndRedirect.toBusinessTax
          case _ => Unauthorized(login.render())
        }

  })

}