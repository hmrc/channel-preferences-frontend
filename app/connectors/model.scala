package connectors

import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}

import scala.language.implicitConversions


object EmailVerificationLinkResponse extends Enumeration {
  type EmailVerificationLinkResponse = Value

  val OK, EXPIRED, ERROR = Value
}

object SaPreferenceSimplified {
  implicit val formats = Json.format[SaPreferenceSimplified]
}

case class SaPreferenceSimplified(digital: Boolean, email: Option[String] = None)

case class UpdateEmail(digital: Boolean, email: Option[String])

object UpdateEmail {
  implicit def formats: Format[UpdateEmail] = Json.format[UpdateEmail]
}

object SaEmailPreference {

  implicit val formats = Json.format[SaEmailPreference]

  object Status {
    val pending = "pending"
    val bounced = "bounced"
    val verified = "verified"
  }

}

case class SaEmailPreference(email: String, status: String, mailboxFull: Boolean = false,
                             message: Option[String] = None, linkSent: Option[LocalDate] = None)


object SaPreference {
  implicit def formats(implicit saEmailPreferenceFormat: Format[SaEmailPreference]): Format[SaPreference] =
    Json.format[SaPreference]
}

case class SaPreference(digital: Boolean, email: Option[SaEmailPreference] = None)


object ValidateEmail {
  implicit val formats = Json.format[ValidateEmail]
}

case class ValidateEmail(token: String)