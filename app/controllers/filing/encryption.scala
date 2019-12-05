package controllers.filing

import java.net.URLDecoder

import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Mode.Mode
import play.api.Play.current
import play.api.mvc.{Action, AnyContent, Request, Results}
import play.api.{Configuration, Logger, Play}
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.config.RunMode

import scala.concurrent.Future

private[filing] case class TokenExpiredException(token: String, time: Long) extends Exception(s"Token expired: $token. Timestamp: $time, Now: ${DateTime.now(DateTimeZone.UTC).getMillis}")

private[filing] case class Token(utr: SaUtr, timestamp: Long, encryptedToken: String)

private[filing] trait TokenEncryption extends Decrypter {

  val base64 = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$"
  val baseConfigKey = "sso.encryption"

  def decryptToken(encryptedToken: String, timeout: Int): Token = {
    val tokenAsString =
      if (encryptedToken.matches(base64)) decrypt(Crypted(encryptedToken))
      else decrypt(Crypted(URLDecoder.decode(encryptedToken, "UTF-8")))

    val (utr, time) = tokenAsString.value.split(":") match {
      case Array(u, t) => (u.trim, t.trim.toLong)
    }
    if (currentTime.minusMinutes(timeout).isAfter(time)) throw TokenExpiredException(encryptedToken, time)
    else Token(SaUtr(utr.trim), time, encryptedToken)
  }

  def currentTime: DateTime = DateTime.now(DateTimeZone.UTC)
}

private[filing] object TokenEncryption extends TokenEncryption with CompositeSymmetricCrypto with KeysFromConfig {
 override def configuration = Play.configuration
}

private[filing] object DecryptAndValidate extends Results with RunMode {
  private lazy val tokenTimeout = Play.configuration.getInt(s"govuk-tax.$env.portal.tokenTimeout").getOrElse(240)

  def apply(encryptedToken: String, returnUrl: Uri)(action: Token => (Action[AnyContent])) = Action.async {
    request: Request[AnyContent] =>
      try {
        implicit val token = TokenEncryption.decryptToken(encryptedToken, tokenTimeout)
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

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

trait KeysFromConfig {
  this: CompositeSymmetricCrypto =>

  val baseConfigKey: String

  def configuration: Configuration

  override protected val currentCrypto = {
    val configKey = baseConfigKey + ".key"
    val currentEncryptionKey = configuration.getString(configKey).getOrElse {
      Logger.error(s"Missing required configuration entry: $configKey")
      throw new SecurityException(s"Missing required configuration entry: $configKey")
    }
    aesCrypto(currentEncryptionKey)
  }

  override protected val previousCryptos = {
    val configKey = baseConfigKey + ".previousKeys"
    val previousEncryptionKeys = configuration.getStringSeq(configKey).getOrElse(Seq.empty)
    previousEncryptionKeys.map(aesCrypto)
  }

  private def aesCrypto(key: String) = {
    try {
      val crypto = new AesCrypto {
        override val encryptionKey = key
      }
      crypto.decrypt(crypto.encrypt(PlainText("assert-valid-key")))
      crypto
    } catch {
      case e: Exception => Logger.error(s"Invalid encryption key: $key", e); throw new SecurityException("Invalid encryption key", e)
    }
  }
}

case class CryptoWithKeysFromConfig(baseConfigKey: String, configuration: Configuration = Play.current.configuration) extends CompositeSymmetricCrypto with KeysFromConfig
