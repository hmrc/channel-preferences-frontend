package controllers.common

import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import views.html._

class HomeController
  extends BaseController
  with ActionWrappers {

  def landing = UnauthorisedAction {
    implicit request =>
      Redirect(routes.LoginController.login()).withNewSession
  }

  def home = AuthorisedForIdaAction() {
    implicit user =>
      implicit request =>
        user.regimes match {
          case RegimeRoots(Some(paye), None, None, None, None, None) => FrontEndRedirect.forSession(session)
          case RegimeRoots(None, Some(sa), _, _, _, _) => FrontEndRedirect.toBusinessTax
          case RegimeRoots(None, _, Some(vat), _, _, _) => FrontEndRedirect.toBusinessTax
          case RegimeRoots(None, _, _, Some(epaye), _, _) => FrontEndRedirect.toBusinessTax
          case RegimeRoots(None, _, _, _, Some(ct), _) => FrontEndRedirect.toBusinessTax
          case _ => Unauthorized(login.render())
        }
  }

}