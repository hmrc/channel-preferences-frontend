package controllers.sa


import hostcontext.ReturnUrl
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.language.implicitConversions

package object prefs {
  // Workaround for play route compilation bug https://github.com/playframework/playframework/issues/2402
  type EncryptedEmail = Encrypted[EmailAddress]

  implicit def encryptedStringToDecryptedEmail(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[Encrypted[EmailAddress]] =
    new EncryptedQueryBinder[EmailAddress](ApplicationCrypto.QueryParameterCrypto, EmailAddress.apply, _.value)

  implicit def encryptedStringToDecryptedString(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[Encrypted[String]] =
    new EncryptedQueryBinder[String](ApplicationCrypto.QueryParameterCrypto, s => s, s => s)

  implicit def stringToReturnUrlBinder(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[ReturnUrl] =
    stringBinder.transform(toB = ReturnUrl.apply, toA = _.url)
}