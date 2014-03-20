package controllers.sa

import controllers.common.service.FrontEndConfig
import java.net.URLDecoder
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc.{Call, Request, AnyContent, Action}
import scala.concurrent.Future
import com.netaporter.uri.dsl._
import com.netaporter.uri.Uri
import controllers.common.preferences.service.{SsoPayloadCrypto, Token, TokenExpiredException}

package object prefs {

  def DecodeAndWhitelist(encodedReturnUrl: String)(action: (Uri => Action[AnyContent]))(implicit allowedDomains: Set[String]): Action[AnyContent] = {
    Action.async {
      request: Request[AnyContent] =>
        val decodedReturnUrl: Uri = URLDecoder.decode(encodedReturnUrl, "UTF-8")

        if (decodedReturnUrl.host.exists(h => allowedDomains.exists(h.endsWith)))
          action(decodedReturnUrl)(request)
        else {
          Logger.debug(s"Return URL '$encodedReturnUrl' was invalid as it was not on the whitelist")
          Future.successful(BadRequest)
        }
    }
  }

  def DecryptAndValidate(encryptedToken: String, returnUrl: Uri)(action: Token => (Action[AnyContent])): Action[AnyContent] =
    Action.async {
      request: Request[AnyContent] =>
        try {
          implicit val token = SsoPayloadCrypto.decryptToken(encryptedToken, FrontEndConfig.tokenTimeout)
          action(token)(request)
        } catch {
          case e: TokenExpiredException =>
            Logger.error("Unable to validate token", e)
            Future.successful(Redirect(returnUrl))
          case e: Exception =>
            Logger.error("Exception happened while decrypting the token", e)
            Future.successful(Redirect(returnUrl))
        }
    }

  val getSavePrefsCall = controllers.sa.prefs.routes.BizTaxPrefsController.submitPrefsForm()

  val getKeepPaperCall: Call = getSavePrefsCall // FIXME REMOVE!!!

  val businessTaxHome = "/account"
}
