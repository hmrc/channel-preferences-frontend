/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import play.api.data.Form
import play.api.data.Forms._

object SurveyReOptInDeclinedDetailsForm {

  val reasonMaxLength = 500

  def apply(): Form[Data] =
    Form(
      mapping(
        "choice1" -> optional(boolean),
        "choice2" -> optional(boolean),
        "choice3" -> optional(boolean),
        "choice4" -> optional(boolean),
        "choice5" -> optional(boolean),
        "reason"  -> optional(text(maxLength = reasonMaxLength))
      )(Data.apply)(Data.unapply)
    )

  case class Data(
    choice1: Option[Boolean],
    choice2: Option[Boolean],
    choice3: Option[Boolean],
    choice4: Option[Boolean],
    choice5: Option[Boolean],
    reason: Option[String]
  )
}
