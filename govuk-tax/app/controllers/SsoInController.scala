package controllers

import play.api.data.Forms._
import play.api.data._
import microservice.governmentgateway.ValidateTokenRequest
import controllers.service.MicroServices
import play.api.Logger

class SsoInController extends BaseController with ActionWrappers with MicroServices {
  def in = UnauthorisedAction {
    implicit request =>
      val form = Form(tuple(
        "gw" -> text,
        "time" -> text
      ))
      val destUris = request.queryString("dest")
      val destUri = destUris.headOption.get
      val (gw, time) = form.bindFromRequest.get
      val tokenRequest = ValidateTokenRequest(gw, time)
      try {
        val response = governmentGatewayMicroService.validateToken(tokenRequest)
        Redirect(destUri).withSession("userId" -> encrypt(response.authId), "name" -> encrypt(response.name), "token" -> encrypt(response.encodedGovernmentGatewayToken))
      } catch {
        case e: Exception => {
          Logger.info("Failed to validate a token.", e)
          Redirect(destUri).withNewSession
        }
      }
  }
}
