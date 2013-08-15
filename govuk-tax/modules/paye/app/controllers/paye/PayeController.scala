package controllers.paye

import uk.gov.hmrc.microservice.paye.domain._
import org.joda.time.LocalDate
import org.joda.time.{ DateTimeZone, DateTime, DateTimeUtils, LocalDate }
import play.api.data._
import play.api.data.Forms._
import views.html.paye._
import views.formatting.Dates
import scala._
import controllers.common._
import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import java.net.URI
import scala.collection.mutable.ListBuffer
import play.api.Logger
import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import scala.Some
import controllers.paye.DisplayBenefit
import controllers.paye.RemoveBenefitFormData
import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import uk.gov.hmrc.microservice.paye.domain.RecentTransaction
import scala.Some
import controllers.paye.DisplayBenefit
import uk.gov.hmrc.microservice.paye.domain.Employment
import uk.gov.hmrc.microservice.paye.domain.Car
import uk.gov.hmrc.microservice.paye.domain.Benefit
import controllers.paye.RemoveBenefitFormData
import controllers.common.service.MicroServices

class PayeController extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  import uk.gov.hmrc.microservice.paye.domain.{ Employment, Benefit, PayeRegime }

  def home = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(Some(PayeRegime)) {
      implicit user =>
        implicit request =>

          val payeData = user.regimes.paye.get
          val taxYear = currentTaxYear
          val benefits = payeData.benefits(taxYear)

          val acceptedTransactions = payeData.transactionsWithStatusFromDate("accepted", currentDate.minusMonths(1))
          val completedTransactions = payeData.transactionsWithStatusFromDate("completed", currentDate.minusMonths(1))

          // this is safe, the AuthorisedForAction wrapper will have thrown Unauthorised if the PayeRoot data isn't present
          val employments = payeData.employments(taxYear)
          val taxCodes = payeData.taxCodes(taxYear)
          val employmentData: Seq[EmploymentData] =
            toEmploymentData(employments, taxCodes, taxYear, acceptedTransactions, completedTransactions)

          Ok(paye_home(
            name = payeData.name,
            employmentData = employmentData,
            hasBenefits = !benefits.isEmpty,
            numberOfTaxCodes = taxCodes.size)
          )

    }
  }

  private def toEmploymentData(employments: Seq[Employment], taxCodes: Seq[TaxCode], taxYear: Int,
    acceptedTransactions: Seq[TxQueueTransaction],
    completedTransactions: Seq[TxQueueTransaction]): Seq[EmploymentData] = {
    for (e <- employments)
      yield new EmploymentData(e,
      taxCodeWithEmploymentNumber(e.sequenceNumber, taxCodes),
      transactionsWithEmploymentNumber(e.sequenceNumber, taxYear, acceptedTransactions, "accepted"),
      transactionsWithEmploymentNumber(e.sequenceNumber, taxYear, completedTransactions, "completed")
    )
  }

  private def taxCodeWithEmploymentNumber(employmentSequenceNumber: Int, taxCodes: Seq[TaxCode]): Option[TaxCode] = {
    taxCodes.find(tc => { tc.employmentSequenceNumber == employmentSequenceNumber })
  }

  private def transactionsWithEmploymentNumber(employmentSequenceNumber: Int, taxYear: Int, transactions: Seq[TxQueueTransaction], messageCodePrefix: String): Seq[RecentTransaction] = {
    transactions.filter(tx => { tx.employmentSequenceNumber == employmentSequenceNumber && tx.taxYear == taxYear && tx.tags.get.filter(_.startsWith("message.code.")).nonEmpty }).
      map(tx => {
        val messageCodeTags = tx.tags.get.filter(_.startsWith("message.code."))
        val messageCode = messageCodeTags(0).replace("message.code", messageCodePrefix)
        new RecentTransaction(messageCode, tx.statusHistory(0).createdAt.toLocalDate)
      }
      )
  }

  def listBenefits = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    implicit user =>
      implicit request =>
        val taxYear = currentTaxYear
        val benefits = user.regimes.paye.get.benefits(taxYear)
        val employments = user.regimes.paye.get.employments(taxYear)
        // TODO: add lowercase hyphenated formatter
        Ok(paye_benefit_home(matchBenefitWithCorrespondingEmployment(benefits, employments)))
  })

  val localDateMapping = jodaLocalDate
    .verifying("error.paye.benefit.date.next.taxyear", date => date.isBefore(new LocalDate(currentTaxYear + 1, 4, 6)))
    .verifying("error.paye.benefit.date.greater.7.days", date => date.minusDays(7).isBefore(new LocalDate()))
    .verifying("error.paye.benefit.date.previous.taxyear", date => date.isAfter(new LocalDate(currentTaxYear, 4, 5)))

  val updateBenefitForm = Form[RemoveBenefitFormData](
    mapping(
      "withdrawDate" -> localDateMapping,
      "agreement" -> checked("error.paye.remove.carbenefit.accept.agreement")
    )(RemoveBenefitFormData.apply)(RemoveBenefitFormData.unapply)
  )

  def removeCarBenefitToStep1(year: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    implicit user =>
      implicit request =>
        Ok(remove_car_benefit_step1(getCarBenefit(user, employmentSequenceNumber), updateBenefitForm))
  })

  def removeCarBenefitToStep2(year: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    implicit user =>
      implicit request =>
        val db = getCarBenefit(user, employmentSequenceNumber)
        updateBenefitForm.bindFromRequest.fold(
          errors => BadRequest(remove_car_benefit_step1(db, errors)),
          removeBenefitData => {
            val calculationResult = payeMicroService.calculateWithdrawBenefit(db.benefit, removeBenefitData.withdrawDate)
            val revisedAmount = calculationResult.result(db.benefit.taxYear.toString)
            Ok(remove_car_benefit_step2(revisedAmount, db.benefit)).withSession(request.session
              + ("withdraw_date", Dates.shortDate(removeBenefitData.withdrawDate))
              + ("revised_amount", revisedAmount.toString()))
          }
        )
  })

  def removeCarBenefitToStep3(year: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    implicit user =>
      implicit request =>
        val db = getCarBenefit(user, employmentSequenceNumber)
        val payeRoot = user.regimes.paye.get
        val withdrawDate = request.session.get("withdraw_date").get
        val revisedAmount = request.session.get("revised_amount").get
        val transactionId = payeMicroService.removeCarBenefit(payeRoot.nino, payeRoot.version, db.benefit, Dates.parseShortDate(withdrawDate), BigDecimal(revisedAmount))
        Redirect(routes.PayeController.benefitRemoved(year, employmentSequenceNumber, transactionId.get.oid))

  })

  def benefitRemoved(year: Int, employmentSequenceNumber: Int, oid: String) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    implicit user =>
      implicit request => {
        if (txQueueMicroService.transaction(oid, user.regimes.paye.get).isEmpty) { NotFound }
        else {
          Ok(remove_car_benefit_step3(Dates.formatDate(Dates.parseShortDate(request.session.get("withdraw_date").get)), oid))
        }
      }
  })

  import uk.gov.hmrc.microservice.domain.User
  private def getCarBenefit(user: User, employmentSequenceNumber: Int): DisplayBenefit = {
    val taxYear = currentTaxYear
    val benefit = user.regimes.paye.get.benefits(taxYear).find(b => b.employmentSequenceNumber == employmentSequenceNumber && b.car.isDefined)
    matchBenefitWithCorrespondingEmployment(benefit.toList, user.regimes.paye.get.employments(taxYear))(0)
  }

  private def matchBenefitWithCorrespondingEmployment(benefits: Seq[Benefit], employments: Seq[Employment]): Seq[DisplayBenefit] =
    benefits
      .filter { benefit => employments.exists(_.sequenceNumber == benefit.employmentSequenceNumber) }
      .map { benefit: Benefit => DisplayBenefit(employments.find(_.sequenceNumber == benefit.employmentSequenceNumber).get, benefit, benefit.car) }

  private def currentTaxYear = {
    val now = new LocalDate
    val year = now.year.get
    if (now.isBefore(new LocalDate(year, 4, 6))) year - 1 else year
  }

  def currentDate: DateTime = {
    new DateTime(DateTimeZone.UTC)
  }
}

case class DisplayBenefit(employment: Employment, benefit: Benefit, car: Option[Car])
case class RemoveBenefitFormData(withdrawDate: LocalDate, agreement: Boolean)
