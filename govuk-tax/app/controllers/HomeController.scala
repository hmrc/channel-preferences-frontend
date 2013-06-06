package controllers

import microservice.paye.domain.PayeRegime

object HomeController extends HomeController

private[controllers] class HomeController extends BaseController with ActionWrappers {

  def home = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>

        val payeData = user.regime.paye.getOrElse(throw new Exception("Regime paye not found"))
        val taxCodes = payeData.taxCodes.getOrElse(Seq.empty)
        val hasBenefits = payeData.links.get("benefits").isDefined

        Ok(views.html.home(payeData.designatoryDetails.firstName + " " + payeData.designatoryDetails.lastName, taxCodes, hasBenefits))
  }
}
