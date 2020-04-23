/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package connectors

import org.joda.time.LocalDate
import play.api.libs.json._

import scala.language.implicitConversions
import org.joda.time.{ DateTime, LocalDate }
import play.api.libs.json.JodaWrites.{ JodaDateTimeWrites => _, _ }
import play.api.libs.json.JodaReads._

sealed trait EmailVerificationLinkResponse
case object Validated extends EmailVerificationLinkResponse
case class ValidatedWithReturn(returnLinkText: String, returnUrl: String) extends EmailVerificationLinkResponse
case object ValidationExpired extends EmailVerificationLinkResponse
case object WrongToken extends EmailVerificationLinkResponse
case object ValidationError extends EmailVerificationLinkResponse
case class ValidationErrorWithReturn(returnLinkText: String, returnUrl: String) extends EmailVerificationLinkResponse
import play.api.libs.json.JodaReads._
import play.api.libs.json.JodaWrites.{ JodaDateTimeWrites => _, _ }
import model.Language

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

    val reads: Reads[Status] = new Reads[Status] {
      override def reads(json: JsValue): JsResult[Status] = json match {
        case JsString("pending")  => JsSuccess(Pending)
        case JsString("bounced")  => JsSuccess(Bounced)
        case JsString("verified") => JsSuccess(Verified)
        case _                    => JsError()
      }
    }

    val writes: OWrites[Status] = new OWrites[Status] {
      override def writes(o: Status): JsObject = JsString(o.toString.toLowerCase).as[JsObject]
    }
    implicit val formats = OFormat(reads, writes)
  }
  implicit val formats = Json.format[SaEmailPreference]

}

case class SaEmailPreference(
  email: String,
  status: SaEmailPreference.Status,
  mailboxFull: Boolean = false,
  message: Option[String] = None,
  linkSent: Option[LocalDate] = None) {

  implicit class emailPreferenceOps(saEmail: SaEmailPreference) {
    def toEmailPreference(): EmailPreference =
      EmailPreference(
        saEmail.email,
        saEmail.status == SaEmailPreference.Status.Verified,
        saEmail.status == SaEmailPreference.Status.Bounced,
        saEmail.mailboxFull,
        saEmail.linkSent)

  }

}

case class EmailPreference(
  email: String,
  isVerified: Boolean,
  hasBounces: Boolean,
  mailboxFull: Boolean,
  linkSent: Option[LocalDate],
  language: Option[Language] = None
)

object EmailPreference {
  implicit val formats = Json.format[EmailPreference]
}

case class TermsAndConditonsAcceptance(accepted: Boolean, updatedAt: Option[DateTime] = None)
object TermsAndConditonsAcceptance {

  implicit val dateFormatDefault = new Format[DateTime] {
    override def reads(json: JsValue): JsResult[DateTime] = JodaReads.DefaultJodaDateTimeReads.reads(json)
    override def writes(o: DateTime): JsValue = JodaWrites.JodaDateTimeNumberWrites.writes(o)
  }
  implicit val formats = Json.format[TermsAndConditonsAcceptance]
}

case class PreferenceResponse(
  termsAndConditions: Map[String, TermsAndConditonsAcceptance],
  email: Option[EmailPreference]) {
  val genericTermsAccepted: Boolean = termsAndConditions.get("generic").fold(false)(_.accepted)
  val taxCreditsTermsAccepted: Boolean = termsAndConditions.get("taxCredits").fold(false)(_.accepted)
}

object PreferenceResponse {

  val defaultRead = Json.reads[PreferenceResponse]

  val reads = new Reads[PreferenceResponse] {
    override def reads(json: JsValue): JsResult[PreferenceResponse] =
      json.validate(defaultRead) match {
        case jSucc @ JsSuccess(_, _) => jSucc
        case _                       => SaPreference.formats.reads(json).map(_.toNewPreference)
      }
  }

  implicit val formats = OFormat(reads, Json.writes[PreferenceResponse])

  implicit class saPreferenceOps(saPreference: SaPreference) {
    def toNewPreference(): PreferenceResponse = {
      def toNewEmail: (SaEmailPreference => EmailPreference) = { saEmail =>
        EmailPreference(
          saEmail.email,
          saEmail.status == SaEmailPreference.Status.Verified,
          saEmail.status == SaEmailPreference.Status.Bounced,
          saEmail.mailboxFull,
          saEmail.linkSent)
      }

      PreferenceResponse(
        termsAndConditions = Map("generic" -> TermsAndConditonsAcceptance(saPreference.digital)),
        email = saPreference.email.map(toNewEmail))
    }
  }

  implicit class preferenceResponseOps(pref: PreferenceResponse) {
    def lang(): Option[Language] = pref.email.flatMap(_.language)
  }
}

case class SaPreference(digital: Boolean, email: Option[SaEmailPreference] = None)

object SaPreference {
  implicit val formats = Json.format[SaPreference]
}

object ValidateEmail {
  implicit val formats = Json.format[ValidateEmail]
}

case class ValidateEmail(token: String)
