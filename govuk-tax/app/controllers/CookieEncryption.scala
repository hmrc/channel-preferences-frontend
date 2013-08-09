package controllers

import play.api.Play
import org.apache.commons.codec.binary.Base64
import javax.crypto.spec.SecretKeySpec
import service.Encryption
import uk.gov.hmrc.secure.{ SymmetricDecrypter, SymmetricEncrypter }

trait CookieEncryption extends Encryption {

  override lazy val encryptionKey = Play.current.configuration.getString("cookie.encryption.key").get

}
