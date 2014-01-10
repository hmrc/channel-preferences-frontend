package uk.gov.hmrc.common.crypto

import play.api.Play
import uk.gov.hmrc.common.crypto.SymmetricCrypto


trait Encrypter {
  def encrypt(id: String): String
}

trait Decrypter {
  def decrypt(id: String): String
}

case class CryptoWithKeyFromConfig(configKey: String) extends SymmetricCrypto {
  lazy val encryptionKey = Play.current.configuration.getString(configKey).get
}
