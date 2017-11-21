package controllers.external

import connectors.{EmailVerificationLinkResponse, EntityResolverConnector}
import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

class EmailValidationController extends FrontendController {

  implicit lazy val entityResolverConnector :EntityResolverConnector = EntityResolverConnector

  val regex = "([0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12})".r

  def verify(token: String) = Action.async {
    implicit request =>
      token match {
        case regex(_) =>
          entityResolverConnector.updateEmailValidationStatusUnsecured(token) map {
            case (EmailVerificationLinkResponse.Ok, Some((returnUrl, returnText))) => Ok(views.html.sa.prefs.sa_printing_preference_verify_email(Some(returnText), Some(returnUrl)))
            case (EmailVerificationLinkResponse.Ok, _) => Ok(views.html.sa.prefs.sa_printing_preference_verify_email(None, None))
            case (EmailVerificationLinkResponse.Expired, _) => Ok(views.html.sa.prefs.sa_printing_preference_expired_email())
            case (EmailVerificationLinkResponse.WrongToken, _) => Ok(views.html.sa.prefs.sa_printing_preference_wrong_token())
            case (EmailVerificationLinkResponse.Error, _) => BadRequest(views.html.sa.prefs.sa_printing_preference_verify_email_failed())
          }
        case _ => Future.successful(BadRequest(views.html.sa.prefs.sa_printing_preference_verify_email_failed()))
      }
  }
}