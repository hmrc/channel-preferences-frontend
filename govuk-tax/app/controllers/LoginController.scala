package controllers

import play.api.mvc.{ Cookie, Action }

class LoginController extends BaseController with ActionWrappers {

  def login = Action {
    Ok(views.html.login())
  }

  def samlLogin = Action {
    val authRequestFormData = samlMicroService.create
    Ok(views.html.saml_auth_form(authRequestFormData.idaUrl, authRequestFormData.samlRequest))
  }

  def enterAsJohnDensmore = Action {
    Redirect(routes.HomeController.home).withCookies(Cookie("userId", "/auth/oid/jdensmore"))
  }

  def enterAsGeoffFisher = Action {
    Redirect(routes.HomeController.home).withCookies(Cookie("userId", "/auth/oid/gfisher"))
  }

}
