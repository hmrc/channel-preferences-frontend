package controllers

import play.api.mvc.Action

object Saml extends BaseController {

  def create = Action {
    val url = "http://localhost:9000/ida"
    val data = "09860948509209672096209358209845092850298350982035982093850285"
    Ok(views.html.saml_auth_form.render(url, data))
  }

  def validate = Action {
    Ok("Hello")
  }
}
