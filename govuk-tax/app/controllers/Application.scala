package controllers

import play.api.mvc._
import controllers.service.{ TaxUser, Person }

object Application extends BaseController {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def home = Action {
    Ok(views.html.home())
  }

  def WithUserData[A](action: TaxUser => Action[A]): Action[A] = {
    null
  }

}