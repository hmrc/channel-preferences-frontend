package controllers.paye

import org.joda.time.{ DateTimeZone, DateTime, LocalDate }
import scala._
import controllers.common._
import models.paye.{ DisplayBenefits, DisplayBenefit }
import uk.gov.hmrc.microservice.domain.User

class PayeController extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  private[paye] def getBenefitMatching(kind: Int, user: User, employmentSequenceNumber: Int): Option[DisplayBenefit] = {
    val taxYear = currentTaxYear
    val benefit = user.regimes.paye.get.benefits(taxYear).find(
      b => b.employmentSequenceNumber == employmentSequenceNumber && b.benefitType == kind)

    val transactions = user.regimes.paye.get.recentCompletedTransactions

    val matchedBenefits = DisplayBenefits(benefit.toList, user.regimes.paye.get.employments(taxYear), transactions)

    if (matchedBenefits.size > 0) Some(matchedBenefits(0)) else None
  }

  //TODO: Use TaxYearResolver instead
  def currentTaxYear = {
    val now = new LocalDate
    val year = now.year.get

    if (now.isBefore(new LocalDate(year, 4, 6)))
      year - 1
    else
      year
  }

  def currentDate = new DateTime(DateTimeZone.UTC)
}

