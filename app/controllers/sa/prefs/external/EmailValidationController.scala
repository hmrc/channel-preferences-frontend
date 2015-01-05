package controllers.sa.prefs.external

import play.api.mvc.Action
import scala.concurrent.Future
import controllers.common.BaseController
import connectors.{EmailVerificationLinkResponse, PreferencesConnector }

class EmailValidationController extends BaseController {

  implicit lazy val preferencesMicroService :PreferencesConnector = PreferencesConnector

  val regex = "([0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12})".r

  def verify(token: String) = Action.async {
    implicit request =>
      token match {
        case regex(_) =>
          preferencesMicroService.updateEmailValidationStatusUnsecured(token) map {
            case EmailVerificationLinkResponse.Ok => Ok(views.html.sa.prefs.sa_printing_preference_verify_email())
            case EmailVerificationLinkResponse.Expired => Ok(views.html.sa.prefs.sa_printing_preference_expired_email())
            case EmailVerificationLinkResponse.WrongToken => Ok(views.html.sa.prefs.sa_printing_preference_wrong_token())
            case EmailVerificationLinkResponse.Error => BadRequest(views.html.sa.prefs.sa_printing_preference_verify_email_failed())
          }
        case _ => Future.successful(BadRequest(views.html.sa.prefs.sa_printing_preference_verify_email_failed()))
      }
  }
}