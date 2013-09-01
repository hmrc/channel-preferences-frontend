package controllers.paye

import play.api.mvc.{ Result, Request }
import uk.gov.hmrc.microservice.domain.User
import models.paye.DisplayBenefit

trait RemoveBenefitValidator {
  self: PayeController =>

  import models.paye.matchers.transactions.matchesBenefit

  object WithValidatedRequest {

    def apply(action: (Request[_], User, DisplayBenefit) => Result): (Int, User, Request[_], Int, Int) => Result = {
      (kind, user, request, year, employmentSequenceNumber) =>

        val func: (Request[_], User) => Result = kind match {
          case 31 | 29 => {
            if (thereAreNoExistingTransactionsMatching(user, kind, employmentSequenceNumber, year)) {
              getBenefitMatching(kind, user, employmentSequenceNumber) match {
                case Some(benefit) => action(_, _, benefit)
                case _ => redirectToBenefitHome
              }
            } else {
              redirectToBenefitHome
            }
          }
          case _ => redirectToBenefitHome
        }
        func(request, user)
    }

    private def thereAreNoExistingTransactionsMatching(user: User, kind: Int, employmentSequenceNumber: Int, year: Int): Boolean = {
      val transactions = user.regimes.paye.get.recentAcceptedTransactions ++
        user.regimes.paye.get.recentCompletedTransactions
      transactions.find(matchesBenefit(_, kind, employmentSequenceNumber, year)).isEmpty
    }

    private val redirectToBenefitHome: (Request[_], User) => Result = (r, u) => Redirect(routes.BenefitHomeController.listBenefits)
  }
}

