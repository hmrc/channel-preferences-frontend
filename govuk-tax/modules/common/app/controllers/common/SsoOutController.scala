package controllers.common

import service.Encryption
import play.api.libs.json._
import play.api.mvc.{ AnyContent, Request, Action }
import org.joda.time.DateTimeUtils
import config.PortalConfig
import play.api.Play

object SsoPayloadEncryptor extends Encryption {
  val encryptionKey = Play.current.configuration.getString("sso.encryption.key").get
}

class SsoOutController extends BaseController with ActionWrappers with CookieEncryption with SessionTimeoutWrapper {

  def encryptPayload = WithSessionTimeoutValidation(Action {
    implicit request =>

      checkThatTheRequestIsValid

      val destinationUrl = retriveDestinationUrl
      val decryptedEncodedGovernmentGatewayToken = decrypt(request.session.get("token").get)
      val encryptedPayload = SsoPayloadEncryptor.encrypt(generateJsonPayload(decryptedEncodedGovernmentGatewayToken, destinationUrl))
      Ok(encryptedPayload)

  })

  private def retriveDestinationUrl(implicit request: Request[AnyContent]): String = {
    request.queryString.get("destinationUrl") match {
      case Some(Seq(destination)) => destination
      case None => PortalConfig.getDestinationUrl("home")
    }
  }

  private def generateJsonPayload(token: String, dest: String) = {
    Json.stringify(Json.obj(("gw", token), ("dest", dest), ("time", DateTimeUtils.currentTimeMillis())))
  }

  private def checkThatTheRequestIsValid(implicit request: Request[AnyContent]) {

    if (theTokenIsMissing || theDestinationIsInvalid) throw new RuntimeException

    def theDestinationIsInvalid = {
      request.queryString.get("destinationUrl") match {
        case Some(Seq(destination: String)) => destination match {
          case d if d.startsWith(PortalConfig.destinationRoot) => false
          case _ => true // TODO BEWT: Some logging - destination should be in the whitelist
        }
        case None => false
        case _ => true // TODO BEWT: Some logging - should have zero or one desination URL
      }
    }
    def theTokenIsMissing = {
      request.session.get("token") match {
        case Some(_) => false
        case _ => true // TODO BEWT: Some logging - should have a token
      }
    }
  }
}
