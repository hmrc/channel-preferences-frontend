package controllers

import controllers.domain.PayeRegime

object PayeController extends PayeController

private[controllers] class PayeController extends BaseController with ActionWrappers {

  def home = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>
        val payeData = user.regime.paye.getOrElse(throw new Exception("Regime paye not found"))

        Ok(payeData.designatoryDetails.name)
  }
}
