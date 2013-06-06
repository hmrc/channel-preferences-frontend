package controllers

import microservice.paye.domain.PayeRegime

object HomeController extends HomeController

private[controllers] class HomeController extends BaseController with ActionWrappers {

  def home = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>

        // this is safe, the AuthorisedForAction wrapper will have thrown Unauthorised if the PayeRoot data isn't present
        val payeData = user.regime.paye.get

        val taxCodes = payeData.taxCodes
        val hasBenefits = !payeData.benefits.isEmpty

        Ok(views.html.home(payeData.designatoryDetails.name, taxCodes, hasBenefits))
  }
}
