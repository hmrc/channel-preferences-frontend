package model

import play.api.Logger
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.language.implicitConversions

case class Encrypted[T](decryptedValue: T)
object Encrypted {
  implicit def encryptedStringToDecryptedEmail(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[Encrypted[EmailAddress]] =
    new EncryptedQueryBinder[EmailAddress](ApplicationCrypto.QueryParameterCrypto, EmailAddress.apply, _.value)

  implicit def encryptedStringToDecryptedString(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[Encrypted[String]] =
    new EncryptedQueryBinder[String](ApplicationCrypto.QueryParameterCrypto, s => s, s => s)
}

private[model] class EncryptedQueryBinder[T](crypto: Encrypter with Decrypter, fromString: String => T, toString: T => String)(implicit stringBinder: QueryStringBindable[String]) extends QueryStringBindable[Encrypted[T]] {
  override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Encrypted[T]]] =
    stringBinder.bind(key, params).map {
      case Right(encryptedString) =>
        try {
          val decrypted = crypto.decrypt(Crypted(encryptedString))
          try {
            Right(Encrypted(fromString(decrypted.value)))
          } catch {
            case e: IllegalArgumentException =>
              Left(s"$key is not valid")
          }
        } catch {
          case e: Exception =>
            Left(s"Could not decrypt value for $key")
        }
      case Left(f) => Left(f)
    }

  override def unbind(key: String, enc: Encrypted[T]): String = stringBinder.unbind(key, crypto.encrypt(PlainText(toString(enc.decryptedValue))).value)
}