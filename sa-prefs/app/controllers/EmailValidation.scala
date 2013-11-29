package controllers

import play.api.mvc.Results._
import play.api.mvc.Action
import play.mvc.Controller
import uk.gov.hmrc.{EmailVerificationLinkResponse, PreferencesMicroService}
import controllers.service.FrontEndConfig

class EmailValidation extends Controller {

  implicit lazy val preferencesMicroService = new PreferencesMicroService()

  val regex = "([0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12})".r

  def verify(token: String) = Action {
    token match {
      case regex(_) => {
        preferencesMicroService.updateEmailValidationStatus(token) match {
          case EmailVerificationLinkResponse.OK => Ok(views.html.sa_printing_preference_verify_email(FrontEndConfig.portalHome))
          case EmailVerificationLinkResponse.EXPIRED => Ok(views.html.sa_printing_preference_expired_email(FrontEndConfig.portalHome))
          case EmailVerificationLinkResponse.ERROR => BadRequest(views.html.sa_printing_preference_verify_email_failed(FrontEndConfig.portalHome))
        }
      }
      case _ => BadRequest(views.html.sa_printing_preference_verify_email_failed(FrontEndConfig.portalHome))
    }
  }
}