package controllers

class PayeController extends BaseController with ActionWrappers {

  import microservice.paye.domain.{ Employment, Benefit, PayeRoot, PayeRegime }

  def home = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>

        // this is safe, the AuthorisedForAction wrapper will have thrown Unauthorised if the PayeRoot data isn't present
        val payeData = user.regime.paye.get

        Ok(views.html.home(
          name = payeData.name,
          employments = payeData.employments,
          taxCodes = payeData.taxCodes,
          hasBenefits = !payeData.benefits.isEmpty)
        )
  }

  def benefits = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>
        val root: PayeRoot = user.regime.paye.get
        Ok(views.html.benefits(matchBenefitWithCorrespondingEmployment(root.benefits, root.employments)))
  }

  private def matchBenefitWithCorrespondingEmployment(benefits: Seq[Benefit], employments: Seq[Employment]): Seq[(Benefit, Employment)] = {
    benefits.flatMap(benefit => {
      val correspondingEmployment = employments.find(_.sequenceNumber.equals(benefit.employmentSequenceNumber))
      correspondingEmployment match {
        case Some(e) => Some((benefit, e))
        case _ => None
      }
    })
  }

}

