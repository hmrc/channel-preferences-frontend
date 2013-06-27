package controllers

import scala.collection.mutable.ListBuffer
import org.joda.time.LocalDate
import microservice.paye.domain.{ Car, Benefit, Employment }

class PayeController extends BaseController with ActionWrappers {

  import microservice.paye.domain.{ Employment, Benefit, PayeRegime }

  def home = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>

        // this is safe, the AuthorisedForAction wrapper will have thrown Unauthorised if the PayeRoot data isn't present
        val payeData = user.regimes.paye.get

        Ok(views.html.paye_home(
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
        val employments = user.regimes.paye.get.employments
        Ok(views.html.benefits(matchBenefitWithCorrespondingEmployment(benefits, employments)))
  }

  def carBenefit(benefit: Int, car: Int) = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>
        val benefits = user.regimes.paye.get.benefits
        benefits
          .find(_.sequenceNumber == benefit)
          .flatMap(_.cars.find(_.sequenceNumber == car))
          .map(c => Ok(views.html.carBenefits(c)))
          .getOrElse(NotFound)
  }

  private def matchBenefitWithCorrespondingEmployment(benefits: Seq[Benefit], employments: Seq[Employment]): Seq[DisplayBenefit] =
    benefits.flatMap { benefit =>
      if (benefit.cars.isEmpty) DisplayBenefit(employments.find(_.sequenceNumber == benefit.employmentSequenceNumber).get, benefit, None) :: Nil
      else benefit.cars.map(car => DisplayBenefit(employments.find(_.sequenceNumber == benefit.employmentSequenceNumber).get, benefit, Some(car)))
    }

}

case class DisplayBenefit(employment: Employment, benefit: Benefit, car: Option[Car])