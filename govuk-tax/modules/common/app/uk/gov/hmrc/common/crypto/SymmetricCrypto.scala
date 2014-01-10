package uk.gov.hmrc.common.crypto

import org.apache.commons.codec.binary.Base64
import javax.crypto.spec.SecretKeySpec
import uk.gov.hmrc.secure.{ SymmetricDecrypter, SymmetricEncrypter }
import scala.util.{Success, Try}

trait SymmetricCrypto extends Encrypter with Decrypter {

  val encryptionKey: String

  private lazy val encryptionKeyBytes = Base64.decodeBase64(encryptionKey.getBytes("UTF-8"))

  private lazy val secretKey = new SecretKeySpec(encryptionKeyBytes, "AES")

  private val encrypter = new SymmetricEncrypter

  private val decrypter = new SymmetricDecrypter

  override def encrypt(value: String): String = encrypter.encrypt(value, secretKey)

  override def decrypt(value: String): String = decrypter.decrypt(value, secretKey)
}

trait CompositeSymmetricCrypto extends Encrypter with Decrypter {

  protected val currentCrypto: Encrypter with Decrypter

  protected val previousCryptos: Seq[Decrypter]

  override def encrypt(value: String): String = currentCrypto.encrypt(value)

  override def decrypt(value: String): String = {

    val decrypterStream = (currentCrypto +: previousCryptos).toStream

    val message = decrypterStream.map(d => Try(d.decrypt(value))).collectFirst {
      case Success(msg) => msg
    }

    message.getOrElse(throw new SecurityException("Unable to decrypt value"))
  }
}
