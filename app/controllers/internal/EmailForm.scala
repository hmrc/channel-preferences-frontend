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
