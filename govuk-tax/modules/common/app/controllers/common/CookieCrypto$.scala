package controllers.common

import play.api.Play
import controllers.common.service.SymmetricCrypto

trait CookieCrypto extends SymmetricCrypto {
  override lazy val encryptionKey = Play.current.configuration.getString("cookie.encryption.key").get
}

object CookieCrypto extends CookieCrypto