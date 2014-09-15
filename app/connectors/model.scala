package connectors

import controllers.sa.prefs.internal.EmailOptInCohorts.Cohort
import play.api.libs.json.{Format, Json}


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
  implicit def formats = Json.format[UpdateEmail]
}

object SaEmailPreference {

  implicit val formats = Json.format[SaEmailPreference]

  object Status {
    val pending = "pending"
    val bounced = "bounced"
    val verified = "verified"
  }

}

case class SaEmailPreference(email: String, status: String, mailboxFull: Boolean = false, message: Option[String] = None)


object SaPreference {
  implicit def formats(implicit saEmailPreferenceFormat: Format[SaEmailPreference]) =
    Json.format[SaPreference]
}

case class SaPreference(digital: Boolean, email: Option[SaEmailPreference] = None)


object ValidateEmail {
  implicit val formats = Json.format[ValidateEmail]
}

case class ValidateEmail(token: String)