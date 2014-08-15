package controllers.sa.prefs.filing

import java.net.URLDecoder

import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import controllers.common.service.FrontEndConfig
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.mvc.{Action, AnyContent, Request, Results}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.crypto.{CompositeSymmetricCrypto, Decrypter, KeysFromConfig}

import scala.concurrent.Future

private[filing] case class TokenExpiredException(token: String, time: Long) extends Exception(s"Token expired: $token. Timestamp: $time, Now: ${DateTime.now(DateTimeZone.UTC).getMillis}")

private[filing] case class Token(utr: SaUtr, timestamp: Long, encryptedToken: String)

private[filing] trait TokenEncryption extends Decrypter {

  val base64 = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$"
  val baseConfigKey = "sso.encryption"

  def decryptToken(encryptedToken: String, timeout: Int): Token = {
    val tokenAsString =
      if (encryptedToken.matches(base64)) decrypt(encryptedToken)
      else decrypt(URLDecoder.decode(encryptedToken, "UTF-8"))

    val (utr, time) = tokenAsString.split(":") match {
      case Array(u, t) => (u.trim, t.trim.toLong)
    }
    if (currentTime.minusMinutes(timeout).isAfter(time)) throw TokenExpiredException(encryptedToken, time)
    else Token(SaUtr(utr.trim), time, encryptedToken)
  }

  def currentTime: DateTime = DateTime.now(DateTimeZone.UTC)
}

private[filing] object TokenEncryption extends TokenEncryption with CompositeSymmetricCrypto with KeysFromConfig

private[filing] object DecryptAndValidate extends Results {
  def apply(encryptedToken: String, returnUrl: Uri)(action: Token => (Action[AnyContent])) = Action.async {
    request: Request[AnyContent] =>
      try {
        implicit val token = TokenEncryption.decryptToken(encryptedToken, FrontEndConfig.tokenTimeout)
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
}