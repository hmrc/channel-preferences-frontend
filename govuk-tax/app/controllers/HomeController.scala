package controllers

import microservice.paye.domain.PayeRegime

object HomeController extends HomeController

private[controllers] class HomeController extends BaseController with ActionWrappers {

  def home = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>

        val payeData = user.regime.paye.getOrElse(throw new Exception("Regime paye not found"))
        val taxCodes = payeData.taxCodes.getOrElse(Seq.empty)

        Ok(views.html.home(payeData.designatoryDetails.name, taxCodes))
  }
}
