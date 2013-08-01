package controllers

import play.api.Play
import org.apache.commons.codec.binary.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.nio.charset.StandardCharsets

trait CookieEncryption {

  private lazy val cookieEncryptionKey = Base64.decodeBase64(Play.current.configuration.getString("cookie.encryption.key").get.getBytes("UTF-8"))

  private lazy val secretKey = new SecretKeySpec(cookieEncryptionKey, "AES")

  def encrypt(id: String) = {
    val cipher = Cipher.getInstance("AES")

    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    new String(Base64.encodeBase64(cipher.doFinal(id.getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8)
  }

  def decrypt(id: String): String = {
    val cipher = Cipher.getInstance("AES")

    cipher.init(Cipher.DECRYPT_MODE, secretKey)
    new String(cipher.doFinal(Base64.decodeBase64(id.getBytes(StandardCharsets.UTF_8))))
  }

  def decrypt(id: Option[String]): Option[String] = id.map(decrypt)
}
