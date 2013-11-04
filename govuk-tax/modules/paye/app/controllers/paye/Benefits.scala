package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain.{PayeRootData, Benefit}
import models.paye.matchers.transactions._
import scala.Some
import uk.gov.hmrc.utils.TaxYearResolver

trait Benefits {

  def findExistingBenefit(employmentNumber: Int, benefitType: Int, payeRootData: PayeRootData): Option[Benefit] = {
    val taxYear = TaxYearResolver.currentTaxYear
    payeRootData.taxYearBenefits.find(b => b.benefitType == benefitType && b.employmentSequenceNumber == employmentNumber) match {
      case Some(benef) if thereAreNoExistingTransactionsMatching(benefitType, employmentNumber, taxYear, payeRootData) => Some(benef)
      case _ => None
    }
  }


  private[paye] def thereAreNoExistingTransactionsMatching(kind: Int, employmentSequenceNumber: Int, year: Int,
                                                           payeRootData: PayeRootData): Boolean = {
    val transactions = payeRootData.completedTransactions ++ payeRootData.acceptedTransactions
    transactions.find(matchesBenefit(_, kind, employmentSequenceNumber, year)).isEmpty
  }

}
