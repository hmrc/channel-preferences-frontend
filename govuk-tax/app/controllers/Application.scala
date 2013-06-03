package controllers

import play.api.mvc._

object Application extends BaseController {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def home = Action {
    Ok(views.html.home())
  }

}
