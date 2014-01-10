package uk.gov.hmrc.common.crypto

import play.api.{PlayException, Play}

trait Encrypter {
  def encrypt(id: String): String
}

trait Decrypter {
  def decrypt(id: String): String
}

case class CryptoWithKeyFromConfig(configKey: String) extends SymmetricCrypto {
  lazy val encryptionKey = Play.current.configuration.getString(configKey).getOrElse(throw new SecurityException("Missing required configuration entry: $configKey"))
}
