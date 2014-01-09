package controllers.common

import play.api.Play
import controllers.common.service.SymmetricCrypto

trait EncryptionKeyFromConfig {
  protected val configKey: String
  lazy val encryptionKey = Play.current.configuration.getString(configKey).get
}

object SessionCookieCrypto extends SymmetricCrypto with EncryptionKeyFromConfig {
  override protected val configKey = "cookie.encryption.key"
}

object QueryParameterCrypto extends SymmetricCrypto with EncryptionKeyFromConfig {
  override protected val configKey = "queryParameter.encryption.key"
}

object SsoPayloadCrypto extends SymmetricCrypto with EncryptionKeyFromConfig {
  override protected val configKey = "sso.encryption.key"
}