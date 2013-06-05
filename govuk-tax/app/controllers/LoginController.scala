package controllers

import play.api.mvc.Action

object LoginController extends LoginController

class LoginController extends BaseController with ActionWrappers {

  def login = Action {
    Ok("Hello")
  }
}
