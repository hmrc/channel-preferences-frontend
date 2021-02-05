/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package model

import play.api.Play
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.language.implicitConversions

case class Encrypted[T](decryptedValue: T)
object Encrypted {
  implicit def encryptedStringToDecryptedEmail(implicit
    stringBinder: QueryStringBindable[String]
  ): QueryStringBindable[Encrypted[EmailAddress]] =
    new EncryptedQueryBinder[EmailAddress](
      new ApplicationCrypto(Play.current.configuration.underlying).QueryParameterCrypto,
      EmailAddress.apply,
      _.value
    )

  implicit def encryptedStringToDecryptedString(implicit
    stringBinder: QueryStringBindable[String]
  ): QueryStringBindable[Encrypted[String]] =
    new EncryptedQueryBinder[String](
      new ApplicationCrypto(Play.current.configuration.underlying).QueryParameterCrypto,
      s => s,
      s => s
    )
}

private[model] class EncryptedQueryBinder[T](
  crypto: Encrypter with Decrypter,
  fromString: String => T,
  toString: T => String
)(implicit stringBinder: QueryStringBindable[String])
    extends QueryStringBindable[Encrypted[T]] {
  override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Encrypted[T]]] =
    stringBinder.bind(key, params).map {
      case Right(encryptedString) =>
        try {
          val decrypted = crypto.decrypt(Crypted(encryptedString))
          try Right(Encrypted(fromString(decrypted.value)))
          catch {
            case e: IllegalArgumentException =>
              Left(s"$key is not valid")
          }
        } catch {
          case e: Exception =>
            Left(s"Could not decrypt value for $key")
        }
      case Left(f) => Left(f)
    }

  override def unbind(key: String, enc: Encrypted[T]): String =
    stringBinder.unbind(key, crypto.encrypt(PlainText(toString(enc.decryptedValue))).value)
}
