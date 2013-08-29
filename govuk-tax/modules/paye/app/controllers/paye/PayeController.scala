package controllers.paye

import uk.gov.hmrc.microservice.paye.domain._
import org.joda.time.{ DateTimeZone, DateTime, LocalDate }
import play.api.data._
import play.api.data.Forms._
import views.html.paye._
import views.formatting.Dates
import scala._
import controllers.common._
import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import scala.Some
import uk.gov.hmrc.microservice.paye.domain.Employment
import uk.gov.hmrc.microservice.paye.domain.Car
import uk.gov.hmrc.microservice.paye.domain.Benefit
import play.api.mvc.{ Request, Result }
import uk.gov.hmrc.microservice.domain.User

class PayeController extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  import uk.gov.hmrc.microservice.paye.domain.{ Employment, Benefit, PayeRegime }

  def home = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { user => request => homeAction(user, request) } }

  private[paye] val homeAction: (User, Request[_]) => Result = (user, request) => {
    val payeData = user.regimes.paye.get
    val taxYear = currentTaxYear
    val benefits = payeData.benefits(taxYear)
    val employments = payeData.employments(taxYear)
    val taxCodes = payeData.taxCodes(taxYear)

    val employmentViews: Seq[EmploymentView] = toEmploymentView(employments, taxCodes, taxYear, payeData.recentAcceptedTransactions(), payeData.recentCompletedTransactions())

    Ok(paye_home(PayeOverview(payeData.name, user.userAuthority.previouslyLoggedInAt, payeData.nino, employmentViews, !benefits.isEmpty)))
  }

  private def toEmploymentView(employments: Seq[Employment],
    taxCodes: Seq[TaxCode],
    taxYear: Int,
    acceptedTransactions: Seq[TxQueueTransaction],
    completedTransactions: Seq[TxQueueTransaction]) =

    for (e <- employments) yield EmploymentView(e.employerNameOrReference, e.startDate, e.endDate, taxCodeWithEmploymentNumber(e.sequenceNumber, taxCodes),
      (transactionsWithEmploymentNumber(e.sequenceNumber, taxYear, acceptedTransactions, "accepted") ++ transactionsWithEmploymentNumber(e.sequenceNumber, taxYear, completedTransactions, "completed")).toList)

  def listBenefits = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    implicit user =>
      implicit request =>
        listBenefitsAction(user, request)
  })

  private[paye] val listBenefitsAction: (User, Request[_]) => Result = (user, request) => {
    val taxYear = currentTaxYear
    val benefits = user.regimes.paye.get.benefits(taxYear)
    val employments = user.regimes.paye.get.employments(taxYear)
    val acceptedTransactions = user.regimes.paye.get.recentAcceptedTransactions

    Ok(paye_benefit_home(matchBenefitWithCorrespondingEmployment(benefits, employments, acceptedTransactions)))
  }

  def benefitRemovalForm(kind: Int, year: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    implicit user =>
      implicit request =>
        benefitRemovalFormAction(kind, user, request, year, employmentSequenceNumber)
  })

  private[paye] val benefitRemovalFormAction: (Int, User, Request[_], Int, Int) => Result = (kind, user, request, year, employmentSequenceNumber) => {
    if (kind == 31)
      Ok(remove_car_benefit_form(getBenefit(kind, user, employmentSequenceNumber), updateBenefitForm))
    else
      Ok(remove_benefit_form(getBenefit(kind, user, employmentSequenceNumber), updateBenefitForm))
  }

  def requestBenefitRemoval(kind: Int, year: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    user => request => requestBenefitRemovalAction(kind, user, request, year, employmentSequenceNumber)
  })

  private[paye] val requestBenefitRemovalAction: (Int, User, Request[_], Int, Int) => Result = (kind, user, request, year, employmentSequenceNumber) => {
    val db = getBenefit(kind, user, employmentSequenceNumber)

    updateBenefitForm.bindFromRequest()(request).fold(
      errors => BadRequest(remove_car_benefit_form(db, errors)),
      removeBenefitData => {
        val calculationResult = payeMicroService.calculateWithdrawBenefit(db.benefit, removeBenefitData.withdrawDate)
        val revisedAmount = calculationResult.result(db.benefit.taxYear.toString)

        if (kind == 31) {
          Ok(remove_car_benefit_confirm(revisedAmount, db.benefit)).withSession(request.session
            + ("withdraw_date", Dates.shortDate(removeBenefitData.withdrawDate))
            + ("revised_amount", revisedAmount.toString()))
        } else {
          Ok(remove_benefit_confirm(revisedAmount, db.benefit)).withSession(request.session
            + ("withdraw_date", Dates.shortDate(removeBenefitData.withdrawDate))
            + ("revised_amount", revisedAmount.toString()))
        }
      }
    )
  }

  def confirmBenefitRemoval(kind: Int, year: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    implicit user =>
      implicit request =>
        confirmBenefitRemovalAction(kind, user, request, year, employmentSequenceNumber)
  })

  private[paye] val confirmBenefitRemovalAction: (Int, User, Request[_], Int, Int) => Result = (kind, user, request, year, employmentSequenceNumber) => {
    val db = getBenefit(kind, user, employmentSequenceNumber)
    val payeRoot = user.regimes.paye.get
    val withdrawDate = request.session.get("withdraw_date").get
    val revisedAmount = request.session.get("revised_amount").get
    val uri = kind match {
      case 31 => db.benefit.actions("removeCar")
      case 29 => db.benefit.actions("removeFuel")
      case _ => throw new IllegalArgumentException(s"No action uri found for benefit type ${kind}")
    }
    val transactionId = payeMicroService.removeBenefit(uri, payeRoot.nino, payeRoot.version, db.benefit, Dates.parseShortDate(withdrawDate), BigDecimal(revisedAmount))

    Redirect(routes.PayeController.benefitRemoved(kind, transactionId.get.oid))
  }

  def benefitRemoved(kind: Int, oid: String) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    implicit user =>
      implicit request => benefitRemovedAction(user, request, kind, oid)
  })

  private[paye] val benefitRemovedAction: (User, Request[_], Int, String) => play.api.mvc.Result = (user, request, kind, oid) =>
    if (txQueueMicroService.transaction(oid, user.regimes.paye.get).isEmpty) {
      NotFound
    } else {
      if (kind == 31) {
        Ok(remove_car_benefit_confirmation(Dates.formatDate(Dates.parseShortDate(request.session.get("withdraw_date").get)), oid))
      } else {
        Ok(remove_benefit_confirmation(Dates.formatDate(Dates.parseShortDate(request.session.get("withdraw_date").get)), kind, oid))
      }
    }

  private def taxCodeWithEmploymentNumber(employmentSequenceNumber: Int, taxCodes: Seq[TaxCode]) = {
    val taxCodeOption: Option[TaxCode] = taxCodes.find(tc => tc.employmentSequenceNumber == employmentSequenceNumber)
    if (taxCodeOption.isDefined) {
      taxCodeOption.get.taxCode
    } else {
      "N/A"
    }

  }

  private def transactionsWithEmploymentNumber(employmentSequenceNumber: Int,
    taxYear: Int,
    transactions: Seq[TxQueueTransaction],
    messageCodePrefix: String): Seq[RecentChange] =
    transactions.filter(tx =>
      tx.properties("employmentSequenceNumber").toInt == employmentSequenceNumber &&
        tx.properties("taxYear").toInt == taxYear &&
        tx.tags.get.filter(_.startsWith("message.code.")).nonEmpty
    ).
      map {
        tx =>
          val messageCodeTags = tx.tags.get.filter(_.startsWith("message.code."))
          val messageCode = messageCodeTags(0).replace("message.code", messageCodePrefix)

          RecentChange(
            messageCode,
            tx.statusHistory(0).createdAt.toLocalDate)
      }

  private val localDateMapping = jodaLocalDate
    .verifying("error.paye.benefit.date.next.taxyear", date => date.isBefore(new LocalDate(currentTaxYear + 1, 4, 6)))
    .verifying("error.paye.benefit.date.greater.7.days", date => date.minusDays(7).isBefore(new LocalDate()))
    .verifying("error.paye.benefit.date.previous.taxyear", date => date.isAfter(new LocalDate(currentTaxYear, 4, 5)))

  private val updateBenefitForm = Form[RemoveBenefitFormData](
    mapping(
      "withdrawDate" -> localDateMapping,
      "agreement" -> checked("error.paye.remove.carbenefit.accept.agreement")
    )(RemoveBenefitFormData.apply)(RemoveBenefitFormData.unapply)
  )

  private def getBenefit(kind: Int, user: User, employmentSequenceNumber: Int): DisplayBenefit = {
    val taxYear = currentTaxYear
    val benefit = user.regimes.paye.get.benefits(taxYear).find(
      b => b.employmentSequenceNumber == employmentSequenceNumber && b.benefitType == kind)
    val transactions = user.regimes.paye.get.recentCompletedTransactions
    val matchedBenefits = matchBenefitWithCorrespondingEmployment(benefit.toList, user.regimes.paye.get.employments(taxYear), transactions)

    matchedBenefits(0)
  }

  private def matchBenefitWithCorrespondingEmployment(benefits: Seq[Benefit], employments: Seq[Employment],
    transactions: Seq[TxQueueTransaction]): Seq[DisplayBenefit] = {
    val matchedBenefits = benefits.filter {
      benefit => employments.exists(_.sequenceNumber == benefit.employmentSequenceNumber)
    }

    matchedBenefits.map {
      benefit =>
        DisplayBenefit(employments.find(_.sequenceNumber == benefit.employmentSequenceNumber).get,
          benefit,
          benefit.car,
          transactions.find(transactionMatches(benefit, _))
        )
    }
  }

  def transactionMatches(benefit: Benefit, tx: TxQueueTransaction): Boolean = {
    tx.properties("employmentSequenceNumber").toInt == benefit.employmentSequenceNumber &&
      tx.properties("taxYear").toInt == benefit.taxYear &&
      tx.properties("benefitType").toInt == benefit.benefitType &&
      tx.tags.get.filter(_.startsWith("message.code.")).nonEmpty
  }

  private def currentTaxYear = {
    val now = new LocalDate
    val year = now.year.get

    if (now.isBefore(new LocalDate(year, 4, 6)))
      year - 1
    else
      year
  }

  def currentDate = new DateTime(DateTimeZone.UTC)
}

case class DisplayBenefit(employment: Employment,
  benefit: Benefit,
  car: Option[Car],
  transaction: Option[TxQueueTransaction])

case class RemoveBenefitFormData(withdrawDate: LocalDate,
  agreement: Boolean)

case class PayeOverview(name: String, lastLogin: Option[DateTime], nino: String, employmentViews: Seq[EmploymentView], hasBenefits: Boolean)
case class EmploymentView(companyName: String, startDate: LocalDate, endDate: Option[LocalDate], taxCode: String, recentChanges: Seq[RecentChange])
case class RecentChange(messageCode: String, timeOfChange: LocalDate)
