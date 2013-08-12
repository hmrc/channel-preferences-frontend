package controllers

import play.api.data.Forms._
import play.api.data._
import microservice.governmentgateway.ValidateTokenRequest
import controllers.service.MicroServices
import play.api.Logger
import play.api.libs.json.Json

class SsoInController extends BaseController with ActionWrappers with MicroServices with SessionTimeoutWrapper {
  def in = WithNewSessionTimeout(UnauthorisedAction {
    implicit request =>
      val form = Form(single("payload" -> text))
      val (payload) = form.bindFromRequest.get
      val decryptedPayload = SsoPayloadEncryptor.decrypt(payload)
      Logger.debug(s"token: $payload")
      val json = Json.parse(decryptedPayload)
      val token = (json \ "gw").as[String]
      val time = (json \ "time").as[Long]
      val dest = (json \ "dest").as[String]
      val tokenRequest = ValidateTokenRequest(token, time)
      try {
        val response = governmentGatewayMicroService.validateToken(tokenRequest)
        Logger.debug(s"successfully authenticated: $response.name")
        Redirect(dest).withSession("userId" -> encrypt(response.authId), "name" -> encrypt(response.name), "token" -> encrypt(response.encodedGovernmentGatewayToken))
      } catch {
        case e: Exception => {
          Logger.info("Failed to validate a token.", e)
          Redirect(routes.HomeController.landing()).withNewSession
        }
      }
  })
}
