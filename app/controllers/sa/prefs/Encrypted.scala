package controllers.sa.prefs

import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.crypto._

import scala.language.implicitConversions

case class Encrypted[T](decryptedValue: T)

class EncryptedQueryBinder[T](crypto: Encrypter with Decrypter, fromString: String => T, toString: T => String)(implicit stringBinder: QueryStringBindable[String]) extends QueryStringBindable[Encrypted[T]] {
  override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Encrypted[T]]] =
    stringBinder.bind(key, params).map {
      case Right(encryptedString) =>
        try {
          val decrypted = crypto.decrypt(Crypted(encryptedString))
          try {
            Right(Encrypted(fromString(decrypted.value)))
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

  override def unbind(key: String, enc: Encrypted[T]): String = stringBinder.unbind(key, crypto.encrypt(PlainText(toString(enc.decryptedValue))).value)
}