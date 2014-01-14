package controllers.sa.prefs.service

import org.joda.time.{ DateTimeZone, DateTime }
import java.net.URLDecoder
import uk.gov.hmrc.common.crypto._

case class TokenExpiredException(token: String, time: Long) extends Exception(s"Token expired: $token. Timestamp: $time, Now: ${DateTime.now(DateTimeZone.UTC).getMillis}")

trait TokenEncryption extends Decrypter {

  val base64 = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$"
  val baseConfigKey = "sso.encryption"

  def decryptToken(token: String, timeout: Int): String = {
    val decryptedQueryParameters = if (token.matches(base64)) decrypt(token) else decrypt(URLDecoder.decode(token, "UTF-8"))
    val splitQueryParams = decryptedQueryParameters.split(":")
    val utr = splitQueryParams(0).trim
    val time = splitQueryParams(1).trim.toLong
    val currentTime: DateTime = DateTime.now(DateTimeZone.UTC)
    if (currentTime.minusMinutes(timeout).isAfter(time)) throw TokenExpiredException(token, time) else utr
  }
}

object SsoPayloadCrypto extends TokenEncryption with CompositeSymmetricCrypto with KeysFromConfig