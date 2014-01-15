package uk.gov.hmrc.common.crypto

import play.api.{Logger, Play}
import scala.collection.JavaConversions._
import uk.gov.hmrc.crypto.{AesCrypto, CompositeSymmetricCrypto}

trait KeysFromConfig {
  this: CompositeSymmetricCrypto =>

  val baseConfigKey: String

  override protected val currentCrypto = {
    val configKey = baseConfigKey + ".key"
    val currentEncryptionKey = Play.current.configuration.getString(configKey).getOrElse {
      Logger.error(s"Missing required configuration entry: $configKey")
      throw new SecurityException(s"Missing required configuration entry: $configKey")
    }
    aesCrypto(currentEncryptionKey)
  }

  override protected val previousCryptos = {
    val configKey = baseConfigKey + ".previousKeys"
    val previousEncryptionKeys = Play.current.configuration.getStringList(configKey).map(_.toSeq).getOrElse(Seq.empty)
    previousEncryptionKeys.map(aesCrypto)
  }

  private def aesCrypto(key: String) = new AesCrypto {
    override val encryptionKey = key
  }
}

case class CryptoWithKeysFromConfig(baseConfigKey: String) extends CompositeSymmetricCrypto with KeysFromConfig
