package controllers.sa.prefs.service

import org.joda.time.{ DateTimeZone, DateTime }
import java.net.URLDecoder
import uk.gov.hmrc.common.crypto._
import uk.gov.hmrc.crypto.{CompositeSymmetricCrypto, Decrypter}
import uk.gov.hmrc.domain.SaUtr

case class TokenExpiredException(token: String, time: Long) extends Exception(s"Token expired: $token. Timestamp: $time, Now: ${DateTime.now(DateTimeZone.UTC).getMillis}")

case class Token(utr: SaUtr, timestamp: Long, encryptedToken: String)

trait TokenEncryption extends Decrypter {

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

object SsoPayloadCrypto extends TokenEncryption with CompositeSymmetricCrypto with KeysFromConfig