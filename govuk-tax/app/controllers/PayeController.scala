package controllers

import scala.collection.mutable.ListBuffer

class PayeController extends BaseController with ActionWrappers {

  import microservice.paye.domain.{ Employment, Benefit, PayeRoot, PayeRegime }

  def home = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>

        // this is safe, the AuthorisedForAction wrapper will have thrown Unauthorised if the PayeRoot data isn't present
        val payeData = user.regimes.paye.get
        val hasBenefits = !payeData.benefits.isEmpty

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
        val benefits = user.regimes.paye.get.benefits
        Ok(views.html.benefits(benefits))
  }

  private def matchBenefitWithCorrespondingEmployment(benefits: Seq[Benefit], employments: Seq[Employment]): Seq[Tuple2[Benefit, Employment]] =
    benefits.foldLeft(ListBuffer[(Benefit, Employment)]()) {
      (matched, benefit) =>
        {
          employments.find(_.sequenceNumber == benefit.employmentSequenceNumber) match {
            case Some(e: Employment) => matched += Tuple2(benefit, e); matched
            case _ => matched
          }
        }
    }

}

