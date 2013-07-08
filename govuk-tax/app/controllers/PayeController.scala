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

  def carBenefit(year: Int, employmentSequenceNumber: Int) = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>
        val form = Form(single("return_date" -> jodaLocalDate))
        val db = getCarBenefit(user, employmentSequenceNumber)
        Ok(views.html.paye_benefit_car(db, form("return_date")))
  }

  def removeCarBenefit(year: Int, employmentSequenceNumber: Int) = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>
        val db = getCarBenefit(user, employmentSequenceNumber)
        val form = Form(single("return_date" -> jodaLocalDate))
        val boundForm = form.bindFromRequest
        boundForm.fold(
          errors => Ok(views.html.paye_benefit_car(db, errors("return_date"))),
          dateCarWithdrawn => {
            val payeRoot = user.regimes.paye.get
            payeMicroService.removeCarBenefit(payeRoot.nino, payeRoot.version, db.benefit, dateCarWithdrawn)

            Redirect(routes.PayeController.benefitRemoved(year, employmentSequenceNumber))
          }
        )

  }

  def benefitRemoved(year: Int, employmentSequenceNumber: Int) = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>
        val db = getCarBenefit(user, employmentSequenceNumber)
        Ok(views.html.paye_benefit_car_removed(db.car.get.dateCarWithdrawn.get))
  }

  import microservice.domain.User
  private def getCarBenefit(user: User, employmentSequenceNumber: Int): DisplayBenefit = {
    val benefit = user.regimes.paye.get.benefits.find(b => b.employmentSequenceNumber == employmentSequenceNumber && !b.cars.isEmpty)
    matchBenefitWithCorrespondingEmployment(benefit.toList, user.regimes.paye.get.employments)(0)
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