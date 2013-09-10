package controllers.paye

import play.api.mvc.{ Result, Request }
import uk.gov.hmrc.microservice.domain.User
import models.paye.{ BenefitTypes, DisplayBenefit }
import BenefitTypes._
import uk.gov.hmrc.microservice.paye.domain.Benefit

trait RemoveBenefitValidator {
  self: PayeController =>

  import models.paye.matchers.transactions.matchesBenefit

  object WithValidatedRequest {
    def apply(action: (Request[_], User, DisplayBenefit) => Result): (User, Request[_], String, Int, Int) => Result = {
      (user, request, benefitTypes, taxYear, employmentSequenceNumber) =>
        {
          val emptyBenefit = DisplayBenefit(null, Seq.empty, None, None)
          val validBenefit = DisplayBenefit.fromStringAllBenefit(benefitTypes).map {
            kind => getBenefit(user, kind, taxYear, employmentSequenceNumber)
          }
            .filter(_.isDefined).map(_.get)
            .foldLeft(emptyBenefit)((a: DisplayBenefit, b: DisplayBenefit) => mergeDisplayBenefits(a, b))

          if (!validBenefit.benefits.isEmpty) {
            action(request, user, validBenefit)
          } else {
            redirectToBenefitHome(request, user)
          }
        }
    }

    private def getBenefit(user: User, kind: Int, taxYear: Int, employmentSequenceNumber: Int): Option[DisplayBenefit] = {

      kind match {
        case CAR | FUEL => {
          if (thereAreNoExistingTransactionsMatching(user, kind, employmentSequenceNumber, taxYear)) {
            getBenefitMatching(kind, user, employmentSequenceNumber) match {
              case Some(benefit) => Some(benefit)
              case _ => None
            }
          } else {
            None
          }
        }
        case _ => None
      }
    }

    private def mergeDisplayBenefits(db1: DisplayBenefit, db2: DisplayBenefit): DisplayBenefit = {

      def validOption[A](option1: Option[A], option2: Option[A]): Option[A] = {
        option1 match {
          case Some(value) => option1
          case None => option2
        }
      }

      db1.copy(
        benefits = db1.benefits ++ db2.benefits,
        car = validOption(db1.car, db2.car),
        transaction = validOption(db1.transaction, db2.transaction),
        employment = if (db1.employment != null) db1.employment else db2.employment
      )
    }

    private def thereAreNoExistingTransactionsMatching(user: User, kind: Int, employmentSequenceNumber: Int, year: Int): Boolean = {
      val transactions = user.regimes.paye.get.recentAcceptedTransactions ++
        user.regimes.paye.get.recentCompletedTransactions
      transactions.find(matchesBenefit(_, kind, employmentSequenceNumber, year)).isEmpty
    }

    private val redirectToBenefitHome: (Request[_], User) => Result = (r, u) => Redirect(routes.BenefitHomeController.listBenefits)
  }

}

