package uk.gov.hmrc.common

import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.domain.Email
import play.api.data.format.Formatter
import play.api.data.{FormError, Forms, Mapping}
import uk.gov.hmrc.common.crypto.{Decrypted, ApplicationCrypto}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

object QueryBinders {

  implicit def stringToEmail(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[Email] = new QueryStringBindable[Email] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Email]] = {
      stringBinder.bind(key, params).map {
        case Right(string) =>
          try {
            Right(Email(string))
          } catch {
            case e: IllegalArgumentException =>
              Left("Not a valid email address")
          }
        case Left(f) => Left(f)
      }
    }

    override def unbind(key: String, email: Email): String = stringBinder.unbind(key, email.value)
  }

  implicit def encryptedStringToDecryptedEmail(implicit stringBinder: QueryStringBindable[String]) =
    new DecryptedEmailQueryStringBindable(ApplicationCrypto.QueryParameterCrypto, stringBinder)
}

class DecryptedEmailQueryStringBindable(crypto: Encrypter with Decrypter, stringBinder: QueryStringBindable[String]) extends QueryStringBindable[Decrypted[Email]] {

  override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Decrypted[Email]]] =
    stringBinder.bind(key, params).map {
      case Right(string) =>
        try {
          val decrypted = crypto.decrypt(string)
          try {
            Right(Decrypted(Email(decrypted)))
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

  override def unbind(key: String, email: Decrypted[Email]): String = stringBinder.unbind(key, crypto.encrypt(email.value.value))

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
