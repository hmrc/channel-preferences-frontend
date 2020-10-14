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
        "choice1" -> boolean,
        "choice2" -> boolean,
        "choice3" -> boolean,
        "choice4" -> boolean,
        "choice5" -> boolean,
        "reason"  -> optional(text(maxLength = reasonMaxLength))
      )(Data.apply)(Data.unapply)
    )

  case class Data(
    choice1: Boolean,
    choice2: Boolean,
    choice3: Boolean,
    choice4: Boolean,
    choice5: Boolean,
    reason: Option[String]
  )
}
