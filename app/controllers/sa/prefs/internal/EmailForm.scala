package controllers.sa.prefs.internal

import controllers.sa.prefs._
import play.api.data.Forms._
import play.api.data._
import uk.gov.hmrc.emailaddress.EmailAddress

object EmailForm {
  val emailWithLimitedLength: Mapping[String] =
    text
      .verifying("error.email", EmailAddress.isValid _)
      .verifying("error.email_too_long", email => email.size < 320)

  def apply() = Form[EmailFormData](mapping(
    "email" -> tuple(
      "main" -> emailWithLimitedLength,
      "confirm" -> optional(text)
    ).verifying("email.confirmation.emails.unequal", email => email._1 == email._2.getOrElse("")),
    "emailVerified" -> optional(text)
  )(EmailFormData.apply)(EmailFormData.unapply))
}
