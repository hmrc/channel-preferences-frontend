package controllers.paye

import uk.gov.hmrc.microservice.paye.domain.{ Car, Benefit, Employment }
import org.joda.time.LocalDate
import uk.gov.hmrc.microservice.paye.domain.{ RecentTransaction, Car, Benefit, Employment }
import org.joda.time.{ DateTimeZone, DateTime, DateTimeUtils, LocalDate }
import play.api.data._
import play.api.data.Forms._
import views.html.paye._
import views.formatting.Dates
import scala._
import scala.Some
import controllers.common._
import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import java.net.URI
import scala.collection.mutable.ListBuffer
import play.api.Logger

class PayeController extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  import uk.gov.hmrc.microservice.paye.domain.{ Employment, Benefit, PayeRegime }

  def home = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(Some(PayeRegime)) {
      implicit user =>
        implicit request =>

          // this is safe, the AuthorisedForAction wrapper will have thrown Unauthorised if the PayeRoot data isn't present
          val payeData = user.regimes.paye.get
          val acceptedTransactions = payeData.transactionsWithStatusFromDate("accepted", currentDate.minusMonths(1))
          val recentAcceptedTxMessages = messagesForTransacions(acceptedTransactions, "accepted")
          val completedTransactions = payeData.transactionsWithStatusFromDate("completed", currentDate.minusMonths(1))
          val recentCompletedTxMessages = messagesForTransacions(completedTransactions, "completed")

          Ok(paye_home(
            name = payeData.name,
            employments = payeData.employments,
            taxCodes = payeData.taxCodes,
            hasBenefits = !payeData.benefits.isEmpty,
            recentAcceptedTransactions = recentAcceptedTxMessages,
            recentCompletedTransactions = recentCompletedTxMessages)
          )
    }
  }

  def messagesForTransacions(transactions: Seq[TxQueueTransaction], messageCodePrefix: String): Seq[RecentTransaction] = for {
    t <- transactions
    messageCodeTags = t.tags.get.filter(_.startsWith("message.code."))
    if messageCodeTags.nonEmpty
    messageCodeTag = messageCodeTags(0)
    messageCode = messageCodeTag.replace("message.code", messageCodePrefix)
    employerNameTags = t.tags.get.filter(_.startsWith("employer.name."))
    if employerNameTags.nonEmpty
    employerNameTag = employerNameTags(0)
    employerName = employerNameTag.replace("employer.name.", "")
    date = t.statusHistory(0).createdAt.toLocalDate
  } yield new RecentTransaction(messageCode, date, employerName)

  def listBenefits = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    implicit user =>
      implicit request =>
        val benefits = user.regimes.paye.get.benefits
        val employments = user.regimes.paye.get.employments
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
      implicit request => Ok(remove_car_benefit_step3(Dates.formatDate(Dates.parseShortDate(request.session.get("withdraw_date").get)), oid))
  })

  import uk.gov.hmrc.microservice.domain.User
  private def getCarBenefit(user: User, employmentSequenceNumber: Int): DisplayBenefit = {
    val benefit = user.regimes.paye.get.benefits.find(b => b.employmentSequenceNumber == employmentSequenceNumber && b.car.isDefined)
    matchBenefitWithCorrespondingEmployment(benefit.toList, user.regimes.paye.get.employments)(0)
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
