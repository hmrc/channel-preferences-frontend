package controllers.sa

import controllers.common.service.FrontEndConfig
import java.net.URLDecoder
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, ApplicationCrypto, Encrypted}
import uk.gov.hmrc.emailaddress.EmailAddress
import scala.concurrent.Future
import com.netaporter.uri.dsl._
import com.netaporter.uri.Uri

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



  // Workaround for play route compilation bug https://github.com/playframework/playframework/issues/2402
  type EncryptedEmail = Encrypted[EmailAddress]

  implicit def encryptedStringToDecryptedEmail(implicit stringBinder: QueryStringBindable[String]) =
    new EncryptedEmailBinder(ApplicationCrypto.QueryParameterCrypto, stringBinder)

}
class EncryptedEmailBinder(crypto: Encrypter with Decrypter, stringBinder: QueryStringBindable[String]) extends QueryStringBindable[Encrypted[EmailAddress]] {
  override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Encrypted[EmailAddress]]] =
    stringBinder.bind(key, params).map {
      case Right(encryptedString) =>
        try {
          val decrypted = crypto.decrypt(encryptedString)
          try {
            Right(Encrypted(EmailAddress(decrypted)))
          } catch {
            case e: IllegalArgumentException =>
              Left("Not a valid email address")
          }
        } catch {
          case e: Exception =>
            Left("Could not decrypt value")
        }
      case Left(f) => Left(f)
    }

  override def unbind(key: String, email: Encrypted[EmailAddress]): String = stringBinder.unbind(key, crypto.encrypt(email.decryptedValue.value))
}