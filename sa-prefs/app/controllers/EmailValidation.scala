package controllers

import play.api.mvc.Results._
import play.api.mvc.Action
import play.mvc.Controller
import uk.gov.hmrc.PreferencesMicroService
import controllers.service.FrontEndConfig

class EmailValidation extends Controller {

  implicit lazy val preferencesMicroService = new PreferencesMicroService()

  def verify(token: String) = Action {
    val response = preferencesMicroService.updateEmailValidationStatus(token)
    if(response) {
      Ok(views.html.sa_printing_preference_verify_email(FrontEndConfig.portalHome))
    }
    else {
      BadRequest(views.html.sa_printing_preference_verify_email_failed(FrontEndConfig.portalHome))
    }

  }
}