package controllers.common.service

import org.apache.commons.codec.binary.Base64
import javax.crypto.spec.SecretKeySpec
import uk.gov.hmrc.secure.{ SymmetricDecrypter, SymmetricEncrypter }

trait Encrypter {
  def encrypt(id: String): String
}
trait Decrypter {
  def decrypt(id: String): String
}

trait SymmetricCrypto extends Encrypter with Decrypter {

  val encryptionKey: String

  private lazy val encryptionKeyBytes = Base64.decodeBase64(encryptionKey.getBytes("UTF-8"))

  private lazy val secretKey = new SecretKeySpec(encryptionKeyBytes, "AES")

  private val encrypter = new SymmetricEncrypter

  private val decrypter = new SymmetricDecrypter

  override def encrypt(id: String): String = encrypter.encrypt(id, secretKey)

  override def decrypt(id: String): String = decrypter.decrypt(id, secretKey)
}
