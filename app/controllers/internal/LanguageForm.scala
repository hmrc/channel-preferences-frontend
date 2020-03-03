/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.internal

import play.api.data.Forms._
import play.api.data._
import model.Language.English

object LanguageForm {
  def apply() =
    Form[Data](
      mapping(
        "language" -> optional(boolean)
      )(Data.apply)(Data.unapply)
    )

  case class Data(language: Option[Boolean])
}
