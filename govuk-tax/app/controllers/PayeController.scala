package controllers

import microservice.paye.domain.PayeRegime

object PayeController extends PayeController

private[controllers] class PayeController extends BaseController with ActionWrappers {

  def home = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>

        // this is safe, the AuthorisedForAction wrapper will have thrown Unauthorised if the PayeRoot data isn't present
        val payeData = user.regime.paye.get

        Ok(payeData.designatoryDetails.name)
  }

  def taxCode = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>

        val payeData = user.regime.paye.getOrElse(throw new Exception("Regime paye not found"))

        val taxCode = payeData.taxCodes.head.code

        Ok(taxCode)
  }
}
