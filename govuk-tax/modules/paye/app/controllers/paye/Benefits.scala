package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain.Benefit
import models.paye.matchers.transactions._
import uk.gov.hmrc.common.microservice.domain.User
import scala.Some
import controllers.common.ActionWrappers
import uk.gov.hmrc.utils.TaxYearResolver

trait Benefits extends ActionWrappers {

  def findExistingBenefit(user: User, employmentNumber: Int, benefitType: Int): Option[Benefit] = {
    val taxYear = TaxYearResolver.currentTaxYear
    val benefits = user.regimes.paye.get.benefits(taxYear)

    benefits.find(b => b.benefitType == benefitType && b.employmentSequenceNumber == employmentNumber) match {
      case Some(benef) if (thereAreNoExistingTransactionsMatching(user, benefitType, employmentNumber, taxYear)) => Some(benef)
      case _ => None
    }
  }


  private[paye] def thereAreNoExistingTransactionsMatching(user: User, kind: Int, employmentSequenceNumber: Int, year: Int): Boolean = {
    val transactions = user.regimes.paye.get.recentAcceptedTransactions ++
      user.regimes.paye.get.recentCompletedTransactions
    transactions.find(matchesBenefit(_, kind, employmentSequenceNumber, year)).isEmpty
  }

}
