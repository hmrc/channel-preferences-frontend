package uk.gov.hmrc.common.crypto

import play.api.{Logger, Play}
import scala.collection.JavaConversions._

trait Encrypter {
  def encrypt(id: String): String
}

trait Decrypter {
  def decrypt(id: String): String
}

case class CryptoWithKeyFromConfig(configKey: String) extends SymmetricCrypto {
  lazy val encryptionKey = Play.current.configuration.getString(configKey).getOrElse {
    Logger.error(s"Missing required configuration entry: $configKey")
    throw new SecurityException(s"Missing required configuration entry: $configKey")
  }
}

case class CompositeCryptoWithKeysFromConfig(baseConfigKey: String) extends CompositeSymmetricCrypto {

  private val currentConfigKey = baseConfigKey + ".key"
  private val previousConfigKey = baseConfigKey + ".previousKeys"

  override protected lazy val currentCrypto = CryptoWithKeyFromConfig(currentConfigKey)

  override protected lazy val previousCryptos = {

    val previousKeys = Play.current.configuration.getStringList(previousConfigKey).map(_.toSeq).getOrElse(Seq.empty)

    previousKeys.map { key => new SymmetricCrypto {
      override val encryptionKey = key
    }}
  }
}
