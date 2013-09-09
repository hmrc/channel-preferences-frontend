package controllers.common

import play.api.data.Forms._
import play.api.data._
import uk.gov.hmrc.microservice.governmentgateway.SsoLoginRequest
import service.{ FrontEndConfig, SsoWhiteListService, MicroServices }
import play.api.Logger
import play.api.libs.json.Json
import java.net.{ MalformedURLException, URISyntaxException, URI }

class SsoInController extends BaseController with ActionWrappers with MicroServices with SessionTimeoutWrapper {

  private[controllers] val ssoWhiteListService = new SsoWhiteListService(FrontEndConfig.domainWhiteList)

  def in = WithNewSessionTimeout(UnauthorisedAction {
    implicit request =>
      val form = Form(single("payload" -> text))
      val (payload) = form.bindFromRequest.get
      val decryptedPayload = SsoPayloadEncryptor.decrypt(payload)
      Logger.debug(s"token: $payload")
      val json = Json.parse(decryptedPayload)
      val token = (json \ "gw").as[String]
      val time = (json \ "time").as[Long]
      val dest = (json \ "dest").asOpt[String]

      checkDestination(dest) match {
        case false => BadRequest
        case true => {
          val tokenRequest = SsoLoginRequest(token, time)
          try {
            val response = governmentGatewayMicroService.ssoLogin(tokenRequest)
            Logger.debug(s"successfully authenticated: $response.name")
            Redirect(dest.get).withSession(
              "userId" -> encrypt(response.authId),
              "name" -> encrypt(response.name),
              "affinityGroup" -> encrypt(response.affinityGroup),
              "token" -> encrypt(response.encodedGovernmentGatewayToken))
          } catch {
            case e: Exception => {
              Logger.info("Failed to validate a token.", e)
              Redirect(routes.HomeController.landing()).withNewSession
            }
          }
        }
      }
  })

  def out = WithNewSessionTimeout(UnauthorisedAction {
    implicit request =>
      Redirect(FrontEndConfig.portalLoggedOutUrl).withNewSession
  })

  private def checkDestination(dest: Option[String]): Boolean = {
    dest match {
      case Some(d) => {
        try {
          val url = URI.create(d).toURL
          ssoWhiteListService.check(url)
        } catch {
          case e: URISyntaxException => Logger.error(s"Requested destination URL is invalid: ${e.getMessage}"); false
          case e: MalformedURLException => Logger.error(s"Requested destination URL is invalid: ${e.getMessage}"); false
          case e: IllegalArgumentException => Logger.error(s"Requested destination URL is invalid: ${e.getMessage}"); false
        }
      }
      case None => Logger.error("Destination field is missing"); false
    }
  }
}
