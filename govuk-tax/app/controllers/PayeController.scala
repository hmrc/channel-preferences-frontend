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

  def listBenefits = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>
        val benefits = user.regimes.paye.get.benefits
        val employments = user.regimes.paye.get.employments
        Ok(views.html.paye_benefit_home(matchBenefitWithCorrespondingEmployment(benefits, employments)))
  }

  def carBenefit(benefitId: Int, carId: Int) = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>
        val form = Form(single("return_date" -> jodaLocalDate))
        getCarBenefit(user, benefitId, carId)
          .map(db => Ok(views.html.paye_benefit_car(db, form("return_date"))))
          .getOrElse(NotFound)
  }

  def removeCarBenefit(benefitId: Int, carId: Int) = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>
        getCarBenefit(user, benefitId, carId)
          .map(db => {
            val form = Form(single("return_date" -> jodaLocalDate))
            val boundForm = form.bindFromRequest
            boundForm.fold(
              errors => Ok(views.html.paye_benefit_car(db, errors("return_date"))),
              formData => Ok(views.html.paye_benefit_car_removed(formData))
            )
          })
          .getOrElse(NotFound)

  }

  import microservice.domain.User
  private def getCarBenefit(user: User, benefitId: Int, carId: Int): Option[DisplayBenefit] = {
    val benefit = user.regimes.paye.get.benefits.find(_.sequenceNumber == benefitId)
    val displayBenefit = matchBenefitWithCorrespondingEmployment(benefit.toList, user.regimes.paye.get.employments)
    displayBenefit
      .filter(_.car.isDefined)
      .find(_.car.get.sequenceNumber == carId)
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