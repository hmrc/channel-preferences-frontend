package controllers.common

import service.Encryption
import play.api.libs.json._
import play.api.mvc.Action
import org.joda.time.DateTimeUtils
import config.PortalConfig
import play.api.Play

object SsoPayloadEncryptor extends Encryption {
  val encryptionKey = Play.current.configuration.getString("sso.encryption.key").get
}

class SsoOutController extends BaseController with ActionWrappers with CookieEncryption with SessionTimeoutWrapper {

  def encryptPayload = WithSessionTimeoutValidation(Action {
    implicit request =>
      val encodedGovernmentGatewayToken = request.session.get("token")
      encodedGovernmentGatewayToken match {
        case Some(token) => {
          val encodedGovernmentGatewayToken = decrypt(token)
          val encryptedPayload = SsoPayloadEncryptor.encrypt(generateJsonPayload(encodedGovernmentGatewayToken, PortalConfig.destinationRoot + "/home"))
          Ok(encryptedPayload)
        }
        case None => BadRequest("Missing government gateway token")
      }
  })

  private def generateJsonPayload(token: String, dest: String) = {
    Json.stringify(Json.obj(("gw", token), ("dest", dest), ("time", DateTimeUtils.currentTimeMillis())))
  }
}
