package controllers.external

import connectors.{EmailVerificationLinkResponse, EntityResolverConnector}
import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

class EmailValidationController extends FrontendController {

  implicit lazy val entityResolverConnector :EntityResolverConnector = EntityResolverConnector

  val regex = "([0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12})".r

  def verify(token: String) = Action.async {
    implicit request =>
      token match {
        case regex(_) =>
          entityResolverConnector.updateEmailValidationStatusUnsecured(token) map {
            case EmailVerificationLinkResponse.Ok => Ok(views.html.sa.prefs.sa_printing_preference_verify_email())
            case EmailVerificationLinkResponse.Expired => Ok(views.html.sa.prefs.sa_printing_preference_expired_email())
            case EmailVerificationLinkResponse.WrongToken => Ok(views.html.sa.prefs.sa_printing_preference_wrong_token())
            case EmailVerificationLinkResponse.Error => BadRequest(views.html.sa.prefs.sa_printing_preference_verify_email_failed())
          }
        case _ => Future.successful(BadRequest(views.html.sa.prefs.sa_printing_preference_verify_email_failed()))
      }
  }
}