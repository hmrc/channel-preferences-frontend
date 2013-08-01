package controllers

import play.api.Play
import org.apache.commons.codec.binary.Base64
import javax.crypto.spec.SecretKeySpec
import uk.gov.hmrc.secure.{ SymmetricDecrypter, SymmetricEncrypter }

trait CookieEncryption {

  private lazy val cookieEncryptionKey = Base64.decodeBase64(Play.current.configuration.getString("cookie.encryption.key").get.getBytes("UTF-8"))

  private lazy val secretKey = new SecretKeySpec(cookieEncryptionKey, "AES")

  private val encrypter = new SymmetricEncrypter

  private val decrypter = new SymmetricDecrypter

  def encrypt(id: String) = encrypter.encrypt(id, secretKey)

  def decrypt(id: String): String = decrypter.decrypt(id, secretKey)

  def decrypt(id: Option[String]): Option[String] = id.map(decrypt)
}
