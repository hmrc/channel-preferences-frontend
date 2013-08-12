package controllers.common

import play.api.Play
import controllers.common.service.Encryption

trait CookieEncryption extends Encryption {

  override lazy val encryptionKey = Play.current.configuration.getString("cookie.encryption.key").get

}
