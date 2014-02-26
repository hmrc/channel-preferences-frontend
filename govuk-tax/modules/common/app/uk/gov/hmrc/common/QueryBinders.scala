package uk.gov.hmrc.common

import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.domain.Email
import play.api.data.format.Formatter
import play.api.data.{FormError, Forms, Mapping}
import uk.gov.hmrc.common.crypto.{Encrypted, ApplicationCrypto}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

object QueryBinders {
  implicit def encryptedStringToDecryptedEmail(implicit stringBinder: QueryStringBindable[String]) =
    new EncryptedEmail(ApplicationCrypto.QueryParameterCrypto, stringBinder)

  class EncryptedEmail(crypto: Encrypter with Decrypter, stringBinder: QueryStringBindable[String]) extends QueryStringBindable[Encrypted[Email]] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Encrypted[Email]]] =
      stringBinder.bind(key, params).map {
        case Right(encryptedString) =>
          try {
            val decrypted = crypto.decrypt(encryptedString)
            try {
              Right(Encrypted(Email(decrypted)))
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

    override def unbind(key: String, email: Encrypted[Email]): String = stringBinder.unbind(key, crypto.encrypt(email.decryptedValue.value))
  }
}

object FormBinders {

  implicit def numberFormatter: Formatter[Int] = new Formatter[Int] {

    override def bind(key: String, params: Map[String, String]): Either[Seq[FormError], Int] = {
      params.get(key).map {
        number =>
          try {
            Right(number.trim.toInt)
          } catch {
            case e: NumberFormatException => Left(Seq(FormError(key, "error.number")))
          }
      }.getOrElse(Left(Seq.empty))
    }

    override def unbind(key: String, value: Int) = Map(key -> value.toString)
  }

  val numberFromTrimmedString: Mapping[Int] = Forms.of[Int](numberFormatter)
}
