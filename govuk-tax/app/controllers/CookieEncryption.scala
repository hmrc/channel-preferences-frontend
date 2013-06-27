package controllers

import play.api.Play
import org.apache.commons.codec.binary.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

trait CookieEncryption {

  lazy val cookieEncryptionKey = Base64.decodeBase64(Play.current.configuration.getString("cookie.encryption.key").get.getBytes("UTF-8"))

  def encrypt(id: String) = {
    val cipher = Cipher.getInstance("AES")
    val secretKey = new SecretKeySpec(cookieEncryptionKey, "AES")

    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    new String(Base64.encodeBase64(cipher.doFinal(id.getBytes())), "UTF-8")
  }

  def decrypt(id: String) = {
    val cipher = Cipher.getInstance("AES")
    val secretKey = new SecretKeySpec(cookieEncryptionKey, "AES")

    cipher.init(Cipher.DECRYPT_MODE, secretKey)
    new String(cipher.doFinal(Base64.decodeBase64(id.getBytes("UTF-8"))))
  }
}
