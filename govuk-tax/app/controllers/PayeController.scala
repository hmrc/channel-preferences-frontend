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
        val root: PayeRoot = user.regime.paye.get

        val benefits = root.benefits
        val employments = root.employments

        Ok(views.html.benefits(matchBenefitWithCorrespondingEmployment(benefits, employments)))
  }

  private def matchBenefitWithCorrespondingEmployment(benefits: Seq[Benefit], employments: Seq[Employment]): Seq[(Benefit, Employment)] = {

    // TODO Refactor this with a simpler solution. EG: use collect instead of flatMap
    benefits.flatMap(benefit => {
      val correspondingEmployment = employments.find(_.sequenceNumber.equals(benefit.employmentSequenceNumber))
      correspondingEmployment match {
        case Some(e) => Some((benefit, e))
        case _ => None
      }
    })
  }

}

