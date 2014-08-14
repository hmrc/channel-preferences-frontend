package controllers.sa

import play.api.mvc._
import uk.gov.hmrc.common.crypto.{ApplicationCrypto, Encrypted}
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.crypto.{Decrypter, Encrypter}

package object prefs {
  // Workaround for play route compilation bug https://github.com/playframework/playframework/issues/2402
  type EncryptedEmail = Encrypted[EmailAddress]

  implicit def encryptedStringToDecryptedEmail(implicit stringBinder: QueryStringBindable[String]) =
    new EncryptedEmailBinder(ApplicationCrypto.QueryParameterCrypto, stringBinder)

}
class EncryptedEmailBinder(crypto: Encrypter with Decrypter, stringBinder: QueryStringBindable[String]) extends QueryStringBindable[Encrypted[EmailAddress]] {
  override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Encrypted[EmailAddress]]] =
    stringBinder.bind(key, params).map {
      case Right(encryptedString) =>
        try {
          val decrypted = crypto.decrypt(encryptedString)
          try {
            Right(Encrypted(EmailAddress(decrypted)))
          } catch {
            case e: IllegalArgumentException =>
              Left("Not a valid email address")
          }
        } catch {
          case e: Exception =>
            Left("Could not decrypt value")
        }
      case Left(f) => Left(f)
    }

  override def unbind(key: String, email: Encrypted[EmailAddress]): String = stringBinder.unbind(key, crypto.encrypt(email.decryptedValue.value))
}