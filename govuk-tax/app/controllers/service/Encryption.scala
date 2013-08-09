package controllers.service

import org.apache.commons.codec.binary.Base64
import javax.crypto.spec.SecretKeySpec
import uk.gov.hmrc.secure.{ SymmetricDecrypter, SymmetricEncrypter }

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
