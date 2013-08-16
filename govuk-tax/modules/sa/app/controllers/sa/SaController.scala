package controllers.sa

import uk.gov.hmrc.microservice.sa.domain.{ SaRegime, SaPerson, SaRoot }
import views.html.sa.sa_personal_details
import controllers.common.{ SsoPayloadEncryptor, SessionTimeoutWrapper, ActionWrappers, BaseController }
import play.api.mvc.Action
import play.api.libs.json.Json
import config.DateTimeProvider
import org.joda.time.DateTime

class SaController extends BaseController with ActionWrappers with SessionTimeoutWrapper with DateTimeProvider {

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
    //TODO - this needs to change to use the utr not the cred id
      val credId = (json \ "credId").as[String]
      val time = (json \ "time").as[Long]

      val currentTime: DateTime = now()
      if (currentTime.minusMinutes(5).isAfter(time)) BadRequest
      else {
        authMicroService.preferences(credId) match {
          case None => NoContent
          case Some(pref) => pref.sa match {
            case Some(sa) if sa.digitalNotifications.isDefined => NoContent
            //TODO - add real text in here
            case _ => Ok("some text")
          }
        }
      }
  }
}
