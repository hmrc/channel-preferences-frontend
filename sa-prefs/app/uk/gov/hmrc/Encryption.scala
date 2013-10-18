package uk.gov.hmrc

import org.apache.commons.codec.binary.Base64
import javax.crypto.spec.SecretKeySpec
import uk.gov.hmrc.secure.{ SymmetricDecrypter, SymmetricEncrypter }
import play.api.Play
import org.joda.time.{ DateTimeZone, DateTime }
import java.net.{ URLDecoder, URLEncoder }

trait Encryption {

  val encryptionKey: String

  private lazy val encryptionKeyBytes = Base64.decodeBase64(encryptionKey.getBytes("UTF-8"))

  private lazy val secretKey = new SecretKeySpec(encryptionKeyBytes, "AES")

  private val encrypter = new SymmetricEncrypter

  private val decrypter = new SymmetricDecrypter

  def encrypt(id: String) = encrypter.encrypt(id, secretKey)

  def decrypt(id: String): String = decrypter.decrypt(id, secretKey)

  def decrypt(id: Option[String]): Option[String] = id.map(decrypt)

}

case class TokenExpiredException(token: String, time: Long) extends Exception(s"Token expired: $token. Timestamp: $time, Now: ${DateTime.now(DateTimeZone.UTC).getMillis}")

trait TokenEncryption extends Encryption {

  val base64 = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$"

  def decryptToken(token: String, timeout: Int): String = {
    val decryptedQueryParameters = if (token.matches(base64)) decrypt(token) else decrypt(URLDecoder.decode(token, "UTF-8"))
    val splitQueryParams = decryptedQueryParameters.split(":")
    val utr = splitQueryParams(0).trim
    val time = splitQueryParams(1).trim.toLong
    val currentTime: DateTime = DateTime.now(DateTimeZone.UTC)
    if (currentTime.minusMinutes(timeout).isAfter(time)) throw TokenExpiredException(token, time) else utr
  }
}