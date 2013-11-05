package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain.{PayeRootData, Benefit}
import models.paye.Matchers.transactions._
import scala.Some
import uk.gov.hmrc.utils.TaxYearResolver

trait Benefits {

  def findExistingBenefit(employmentNumber: Int, benefitType: Int, payeRootData: PayeRootData): Option[Benefit] = {
    val taxYear = TaxYearResolver.currentTaxYear
    payeRootData.taxYearBenefits.find(b => b.benefitType == benefitType && b.employmentSequenceNumber == employmentNumber) match {
      case Some(benef) if thereAreNoExistingRemoveBenefitTransactionsMatching(benefitType, employmentNumber, taxYear, payeRootData) => Some(benef)
      case _ => None
    }
  }


  private[paye] def thereAreNoExistingRemoveBenefitTransactionsMatching(kind: Int, employmentSequenceNumber: Int, year: Int,
                                                           payeRootData: PayeRootData): Boolean = {
    val transactions = payeRootData.completedTransactions ++ payeRootData.acceptedTransactions
    transactions.find(matchesBenefit(_, kind, employmentSequenceNumber, year, "removeBenefits")).isEmpty
  }

}
