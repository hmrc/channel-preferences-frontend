package controllers.sa

import uk.gov.hmrc.microservice.sa.domain.{ SaRegime, SaPerson, SaRoot }
import views.html.sa.sa_personal_details
import controllers.common.{ SsoPayloadEncryptor, SessionTimeoutWrapper, ActionWrappers, BaseController }
import play.api.mvc.Action
import play.api.libs.json.Json
import util.{ Success, Failure, Try }
import uk.gov.hmrc.microservice.MicroServiceException
import uk.gov.hmrc.common.microservice.auth.domain.Preferences

class SaController extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  def details = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction(Some(SaRegime)) {
    implicit user =>
      implicit request =>

        val userData: SaRoot = user.regimes.sa.get

        userData.personalDetails match {
          case Some(person: SaPerson) => Ok(sa_personal_details(userData.utr, person, user.nameFromGovernmentGateway.getOrElse("")))
          case _ => NotFound //todo this should really be an error page
        }
  })

  def checkPrintPreferences(encryptedJson: String) = UnauthorisedAction {
    implicit request =>
    val decryptedJson = SsoPayloadEncryptor.decrypt(encryptedJson)
    val json = Json.parse(decryptedJson)
    val credId = (json \ "credId").as[String]

    Try(authMicroService.preferences(credId)) match {
      case Success(None) => Ok("some text")
      case Success(Some(pref)) => pref.sa match {
        case None => Ok("some text")
        case Some(sa) if sa.digitalNotifications.isEmpty => Ok("some text")
        case _ => Ok
      }
      case Failure(e: MicroServiceException) => BadRequest
    }
  }


}
