package controllers

import play.api.mvc.Controller

class Personal extends Controller with ActionWrappers {

  def home = AuthenticatedPersonAction { implicit request =>
    {
      request.personId
      Ok("Hello")
    }
  }
}
