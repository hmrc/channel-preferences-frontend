package controllers

import play.api.mvc.Results._
import play.api.mvc.Action
import play.mvc.Controller
import uk.gov.hmrc.PreferencesMicroService

class EmailValidation extends Controller {

  implicit lazy val preferencesMicroService = new PreferencesMicroService()

  def verify(token: String) = Action {

    Ok(views.html.sa_printing_preference_verify_email("http://localhost:8080/portal/login"))
  }

  def notRequested(token: String) = Action {
    Ok(views.html.sa_printing_preference_email_not_requested("http://localhost:8080/portal/login"))
  }

}
