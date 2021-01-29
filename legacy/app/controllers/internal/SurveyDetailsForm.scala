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

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

object SurveyReOptInDeclinedDetailsForm {

  val reasonMaxLength = 3000

  def apply(): Form[Data] =
    Form(
      mapping(
        "choice-0305d33f-2e8d-4cb2-82d2-52132fc325fe" -> optional(boolean),
        "choice-ce34aa17-df2a-44fb-9d5c-4d930396483a" -> optional(boolean),
        "choice-d0edb491-6dcb-48a8-aeca-b16f01c541a5" -> optional(boolean),
        "choice-1e825e7d-6fc8-453f-8c20-1a7ed4d84ea5" -> optional(boolean),
        "choice-15d28c3f-9f33-4c44-aefa-165fc84b5e23" -> optional(boolean),
        "reason"                                      -> optional(text(maxLength = reasonMaxLength)),
        "submissionType"                              -> optional(text)
      )(Data.apply)(Data.unapply)
    )

  case class Data(
    `choice-0305d33f-2e8d-4cb2-82d2-52132fc325fe`: Option[Boolean],
    `choice-ce34aa17-df2a-44fb-9d5c-4d930396483a`: Option[Boolean],
    `choice-d0edb491-6dcb-48a8-aeca-b16f01c541a5`: Option[Boolean],
    `choice-1e825e7d-6fc8-453f-8c20-1a7ed4d84ea5`: Option[Boolean],
    `choice-15d28c3f-9f33-4c44-aefa-165fc84b5e23`: Option[Boolean],
    reason: Option[String],
    submissionType: Option[String]
  )
}

case class QuestionAnswer(question: String, answer: String)
object QuestionAnswer {
  implicit val formats = Json.format[QuestionAnswer]
}

case class EventDetail(
  submissionType: String,
  utr: String,
  nino: String,
  language: String,
  choices: Map[String, QuestionAnswer],
  reason: String)
object EventDetail {
  implicit val formats = Json.format[EventDetail]
}
