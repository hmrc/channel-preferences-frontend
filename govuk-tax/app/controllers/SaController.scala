package controllers

import play.api.mvc.Action
import play.api.mvc.Results.Ok

class SaController extends BaseController with ActionWrappers {

  def home = AuthorisedForAction {
    implicit user =>
      implicit request =>

        val userData = "Geoff Fisher"

        Ok(views.html.sa_home(userData))

  }

}
