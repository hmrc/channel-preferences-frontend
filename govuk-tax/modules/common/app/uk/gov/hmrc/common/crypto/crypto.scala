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

  private val currentConfigKey = baseConfigKey + ".current"
  private val previousConfigKey = baseConfigKey + ".previous"

  override protected lazy val currentCrypto = CryptoWithKeyFromConfig(currentConfigKey)

  override protected lazy val previousCryptos = {

    val previousKeys = Play.current.configuration.getStringList(previousConfigKey).getOrElse {
      Logger.error(s"Missing required configuration entry: $previousConfigKey")
      throw new SecurityException(s"Missing required configuration entry: $previousConfigKey")
    }

    previousKeys.map { key => new SymmetricCrypto {
      override val encryptionKey = key
    }}
  }
}
