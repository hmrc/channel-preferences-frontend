package controllers.sa.prefs

import play.api.mvc.Action
import uk.gov.hmrc.{EmailVerificationLinkResponse, PreferencesConnector}
import scala.concurrent.Future
import controllers.common.BaseController

class EmailValidation extends BaseController {

  implicit lazy val preferencesMicroService = new PreferencesConnector()

  val regex = "([0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12})".r

  def verify(token: String) = Action.async {
    implicit request =>
      token match {
        case regex(_) =>
          preferencesMicroService.updateEmailValidationStatus(token) map {
            case EmailVerificationLinkResponse.OK => Ok(views.html.sa.prefs.sa_printing_preference_verify_email())
            case EmailVerificationLinkResponse.EXPIRED => Ok(views.html.sa.prefs.sa_printing_preference_expired_email())
            case EmailVerificationLinkResponse.ERROR => BadRequest(views.html.sa.prefs.sa_printing_preference_verify_email_failed())
          }
        case _ => Future.successful(BadRequest(views.html.sa.prefs.sa_printing_preference_verify_email_failed()))
      }
  }
}