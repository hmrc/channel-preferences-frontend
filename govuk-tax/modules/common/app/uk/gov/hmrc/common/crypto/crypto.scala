package uk.gov.hmrc.common.crypto

import play.api.{Logger, Play}
import scala.collection.JavaConversions._
import uk.gov.hmrc.crypto.{SymmetricCrypto, CompositeSymmetricCrypto}

trait KeysFromConfig {
  this: CompositeSymmetricCrypto =>

  val baseConfigKey: String

  override protected val currentCrypto = {
    val configKey = baseConfigKey + ".key"
    val currentEncryptionKey = Play.current.configuration.getString(configKey).getOrElse {
      Logger.error(s"Missing required configuration entry: $configKey")
      throw new SecurityException(s"Missing required configuration entry: $configKey")
    }
    symmetricCrypto(currentEncryptionKey)
  }

  override protected val previousCryptos = {
    val configKey = baseConfigKey + ".previousKeys"
    val previousEncryptionKeys = Play.current.configuration.getStringList(configKey).map(_.toSeq).getOrElse(Seq.empty)
    previousEncryptionKeys.map(symmetricCrypto)
  }

  private def symmetricCrypto(key: String) = new SymmetricCrypto {
    override val encryptionKey = key
  }
}

case class CryptoWithKeysFromConfig(baseConfigKey: String) extends CompositeSymmetricCrypto with KeysFromConfig
