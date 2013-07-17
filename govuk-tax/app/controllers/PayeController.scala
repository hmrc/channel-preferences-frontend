package controllers

import microservice.paye.domain.{ Car, Benefit, Employment }
import org.joda.time.LocalDate
import play.api.data._
import play.api.data.Forms._
import views.html.paye._
import views.formatting.Dates

class PayeController extends BaseController with ActionWrappers {

  import microservice.paye.domain.{ Employment, Benefit, PayeRegime }

  def home = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>

        // this is safe, the AuthorisedForAction wrapper will have thrown Unauthorised if the PayeRoot data isn't present
        val payeData = user.regimes.paye.get

        Ok(paye_home(
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
        Ok(paye_benefit_home(matchBenefitWithCorrespondingEmployment(benefits, employments)))
  }

  val localDateMapping = jodaLocalDate verifying ("error.benefit.date.greater.35.days", date => date.minusDays(35).isBefore(new LocalDate()))
  val updateBenefitForm: Form[LocalDate] = Form(single("withdraw_date" -> localDateMapping))

  def removeCarBenefitToStep1(year: Int, employmentSequenceNumber: Int) = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>
        Ok(remove_car_benefit_step1(getCarBenefit(user, employmentSequenceNumber), updateBenefitForm))
  }

  def removeCarBenefitToStep2(year: Int, employmentSequenceNumber: Int) = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>
        val db = getCarBenefit(user, employmentSequenceNumber)
        updateBenefitForm.bindFromRequest.fold(
          errors => BadRequest(remove_car_benefit_step1(db, errors)),
          withdrawDate => {
            val calculationResult = payeMicroService.calculateWithdrawBenefit(db.benefit, withdrawDate)
            val revisedAmount = calculationResult.result(db.benefit.taxYear.toString)
            Ok(remove_car_benefit_step2(revisedAmount, db.benefit)).withSession(request.session
              + ("withdraw_date", Dates.shortDate(withdrawDate))
              + ("revised_amount", revisedAmount.toString()))
          }
        )
  }

  def removeCarBenefitToStep3(year: Int, employmentSequenceNumber: Int) = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request =>
        val db = getCarBenefit(user, employmentSequenceNumber)
        val payeRoot = user.regimes.paye.get
        val withdrawDate = request.session.get("withdraw_date").get
        val revisedAmount = request.session.get("revised_amount").get
        payeMicroService.removeCarBenefit(payeRoot.nino, payeRoot.version, db.benefit, Dates.parseShortDate(withdrawDate), BigDecimal(revisedAmount))
        Redirect(routes.PayeController.benefitRemoved(year, employmentSequenceNumber))

  }

  def benefitRemoved(year: Int, employmentSequenceNumber: Int) = AuthorisedForAction[PayeRegime] {
    implicit user =>
      implicit request => Ok(remove_car_benefit_step3(Dates.formatDate(Dates.parseShortDate(request.session.get("withdraw_date").get))))
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