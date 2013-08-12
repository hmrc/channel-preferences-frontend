package controllers.common

import play.api.data.Forms._
import play.api.data._
import uk.gov.hmrc.microservice.governmentgateway.ValidateTokenRequest
import controllers.common.service.MicroServices
import play.api.Logger

class SsoInController extends BaseController with ActionWrappers with MicroServices with SessionTimeoutWrapper {
  def in = WithNewSessionTimeout(UnauthorisedAction {
    implicit request =>
      val form = Form(tuple(
        "gw" -> text,
        "time" -> text,
        "dest" -> text
      ))
      val (gw, time, destUri) = form.bindFromRequest.get
      Logger.debug(s"time: $time dest: $destUri")
      Logger.debug(s"token: $gw")
      val tokenRequest = ValidateTokenRequest(gw, time)
      try {
        val response = governmentGatewayMicroService.validateToken(tokenRequest)
        Logger.debug(s"successfully authenticated: $response.name")
        Redirect(destUri).withSession("userId" -> encrypt(response.authId), "name" -> encrypt(response.name), "token" -> encrypt(response.encodedGovernmentGatewayToken))
      } catch {
        case e: Exception => {
          Logger.info("Failed to validate a token.", e)
          Redirect(routes.HomeController.landing()).withNewSession
        }
      }
  })
}
