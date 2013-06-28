package controllers

import microservice.paye.domain.{ Car, Benefit, Employment }
import org.joda.time.LocalDate
import play.api.data._
import play.api.data.Forms._

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

  def carBenefit(benefitId: Int, carId: Int) = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>
        val benefit = user.regimes.paye.get.benefits.find(_.sequenceNumber == benefitId)
        val displayBenefit = matchBenefitWithCorrespondingEmployment(benefit.toList, user.regimes.paye.get.employments)
        displayBenefit
          .filter(_.car.isDefined)
          .find(_.car.get.sequenceNumber == carId)
          .map(db => Ok(views.html.carBenefits(db)))
          .getOrElse(NotFound)
  }

  def removeBenefit(benefitId: Int, carId: Int) = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>
        val form = Form(single("return_date" -> jodaLocalDate))
        val boundForm = form.bindFromRequest
        boundForm.fold(
          errors => {
            println(errors)
            BadRequest
          },
          formData => {
            val benefit = user.regimes.paye.get.benefits.find(_.sequenceNumber == benefitId)
            val displayBenefit = matchBenefitWithCorrespondingEmployment(benefit.toList, user.regimes.paye.get.employments)
            displayBenefit
              .filter(_.car.isDefined)
              .find(_.car.get.sequenceNumber == carId)
              .map(db => Ok(views.html.benefit_removed(formData)))
              .getOrElse(BadRequest)
          }
        )

  }

  private def matchBenefitWithCorrespondingEmployment(benefits: Seq[Benefit], employments: Seq[Employment]): Seq[DisplayBenefit] =
    benefits
      .filter { benefit => employments.exists(_.sequenceNumber == benefit.employmentSequenceNumber) }
      .flatMap { benefit =>
        if (benefit.cars.isEmpty) DisplayBenefit(employments.find(_.sequenceNumber == benefit.employmentSequenceNumber).get, benefit, None) :: Nil
        else benefit.cars.map(car => DisplayBenefit(employments.find(_.sequenceNumber == benefit.employmentSequenceNumber).get, benefit, Some(car)))
      }

}

case class DisplayBenefit(employment: Employment, benefit: Benefit, car: Option[Car])