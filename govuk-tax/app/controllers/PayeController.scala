package controllers

import microservice.paye.domain._

class PayeController extends BaseController with ActionWrappers {

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

  def benefits = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>
        val benefits = user.regime.paye.get.benefits
        Ok(views.html.benefits(benefits))
  }

}
