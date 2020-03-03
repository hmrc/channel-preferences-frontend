/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import play.api.data.Forms._
import play.api.data._
import uk.gov.hmrc.emailaddress.EmailAddress

object EmailForm {
  val emailWithLimitedLength: Mapping[String] =
    text
      .verifying("error.email", EmailAddress.isValid _)
      .verifying("error.email_too_long", email => email.size < 320)

  def apply() =
    Form[Data](
      mapping(
        "email" -> tuple(
          "main"    -> emailWithLimitedLength,
          "confirm" -> optional(text)
        ).verifying("email.confirmation.emails.unequal", email => email._1 == email._2.getOrElse("")),
        "emailVerified" -> optional(text)
      )(Data.apply)(Data.unapply))

  import uk.gov.hmrc.emailaddress.EmailAddress

  case class Data(email: (String, Option[String]), emailVerified: Option[String]) {
    lazy val isEmailVerified = emailVerified.contains("true")

    def mainEmail = email._1
  }
  object Data {
    def apply(emailAddress: Option[EmailAddress]): Data = Data(
      email = emailAddress.map(e => (e.value, Some(e.value))).getOrElse(("", None)),
      emailVerified = None
    )
  }
}
