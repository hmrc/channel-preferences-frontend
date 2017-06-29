package controllers.internal

import play.api.data.Form
import play.api.data.Forms._

object OptInOrOutForm {
  def apply() = Form[Data](mapping(
    "opt-in" -> optional(boolean).verifying("sa_printing_preference.opt_in_choice_required", _.isDefined)
  )(Data.apply)(Data.unapply))

  case class Data(optedIn: Option[Boolean])
}


object OptInOrOutTaxCreditsForm {
  def apply() = Form[Data](mapping(
    "termsAndConditions.opt-in" -> optional(boolean).verifying("sa_printing_preference.opt_in_choice_required", _.isDefined)
  )(Data.apply)(Data.unapply))

  case class Data(optedIn: Option[Boolean])
}
