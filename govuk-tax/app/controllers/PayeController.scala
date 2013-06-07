package controllers

import microservice.paye.domain.{ Benefit, PayeRegime }
import microservice.domain.User
import play.api.mvc.Action

object PayeController extends PayeController

private[controllers] class PayeController extends BaseController with ActionWrappers {

  def home = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>

        // this is safe, the AuthorisedForAction wrapper will have thrown Unauthorised if the PayeRoot data isn't present
        val payeData = user.regime.paye.get
        val hasBenefits = !payeData.benefits.isEmpty

        Ok(views.html.home(
          name = payeData.name,
          employments = payeData.employments,
          taxCodes = payeData.taxCodes,
          hasBenefits = hasBenefits)
        )
  }

}
