package controllers

import play.api.mvc.Results._
import play.api.mvc.Action
import play.mvc.Controller
import uk.gov.hmrc.PreferencesMicroService

class EmailValidation extends Controller {

  implicit lazy val preferencesMicroService = new PreferencesMicroService()

  def verify(token: String) = Action {
    val response = preferencesMicroService.updateEmailValidationStatus(token)
    if(response) {
      Ok(views.html.sa_printing_preference_verify_email("http://localhost:8080/portal/login"))
    }
    else {
      BadRequest(views.html.sa_printing_preference_verify_email_failed("http://localhost:8080/portal/login"))
    }

  }
}