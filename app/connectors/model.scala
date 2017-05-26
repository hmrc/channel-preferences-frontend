package connectors

import org.joda.time.LocalDate
import play.api.libs.json._

import scala.language.implicitConversions


object EmailVerificationLinkResponse extends Enumeration {
  type EmailVerificationLinkResponse = Value

  val Ok, Expired, WrongToken, Error = Value
}

object SaPreferenceSimplified {
  implicit val formats = Json.format[SaPreferenceSimplified]
}

case class SaPreferenceSimplified(digital: Boolean, email: Option[String] = None)

case class UpdateEmail(email: String)

object UpdateEmail {
  implicit def formats: Format[UpdateEmail] = Json.format[UpdateEmail]
}

object SaEmailPreference {

  sealed trait Status
  object Status {
    case object Pending extends Status
    case object Bounced extends Status
    case object Verified extends Status

    implicit val reads: Reads[Status] = new Reads[Status] {
      override def reads(json: JsValue): JsResult[Status] = json match {
        case JsString("pending") => JsSuccess(Pending)
        case JsString("bounced") => JsSuccess(Bounced)
        case JsString("verified") => JsSuccess(Verified)
        case _ => JsError()
      }
    }
  }

  implicit val reads = Json.reads[SaEmailPreference]
}

case class SaEmailPreference(email: String, status: SaEmailPreference.Status, mailboxFull: Boolean = false,
                             message: Option[String] = None, linkSent: Option[LocalDate] = None)


object SaPreference {
  implicit val reads = Json.reads[SaPreference]
}

case class SaPreference(digital: Boolean, email: Option[SaEmailPreference] = None)


object ValidateEmail {
  implicit val formats = Json.format[ValidateEmail]
}

case class ValidateEmail(token: String)

case class PreferenceEmail(address: String, isVerified: Boolean, hasBounces: Boolean)

object PreferenceEmail {
  implicit val formats = Json.format[PreferenceEmail]
}

case class PaperlessService(optedIn: Boolean, terms: String)

object PaperlessService {
  implicit val formats = Json.format[PaperlessService]
}

case class PaperlessPreference(services: Map[String, PaperlessService], email: Option[PreferenceEmail])

object PaperlessPreference {
  implicit val formats = Json.format[PaperlessPreference]
}