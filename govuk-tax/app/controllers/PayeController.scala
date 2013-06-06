package controllers

import microservice.paye.domain.{ Benefit, PayeRegime }
import microservice.domain.User
import play.api.mvc.Action

object PayeController extends PayeController

private[controllers] class PayeController extends BaseController with ActionWrappers {

  def home = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>

        val payeData = user.regime.paye.getOrElse(throw new Exception("Regime paye not found"))

        val employments = payeData.employments.getOrElse(throw new Exception("No employments found"))
        val taxCodes = payeData.taxCodes.getOrElse(throw new Exception("No tax codes found"))
        val hasBenefits = payeData.benefits.isDefined && !payeData.benefits.isEmpty

        Ok(views.html.home(firstName = "John", employments = employments, taxCodes = taxCodes, hasBenefits = hasBenefits))

  }

}
