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

          //val validBenefits = displayBenefit.allBenefits.map( getBenefit(_, user) ).filter(_.isDefined)

          val validBenefits = DisplayBenefit.fromStringAllBenefit(benefitTypes).map { kind =>
            getBenefit(user, kind, taxYear, employmentSequenceNumber)
          }.filter(_.isDefined)

          val hackValidBenefit = validBenefits(0)
          if (hackValidBenefit.isDefined) {
            action(request, user, hackValidBenefit.get)
          } else {
            redirectToBenefitHome(request, user)
          }
          //TODO add dependentbenefits to displaybenefit
        }

      //        case action(list)
      //        case - : redirect
      //      }
      //      case _ => redirectToBenefitHome

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
    //    def one(action: (Request[_], User, DisplayBenefit) => Result): (Int, User, Request[_], Int, Int) => Result = {
    //      (kind, user, request, year, employmentSequenceNumber) =>
    //
    //        val func: (Request[_], User) => Result = kind match {
    //          case CAR | FUEL => {
    //            if (thereAreNoExistingTransactionsMatching(user, kind, employmentSequenceNumber, year)) {
    //              getBenefitMatching(kind, user, employmentSequenceNumber) match {
    //                case Some(benefit) => action(_, _, benefit)
    //                case _ => redirectToBenefitHome
    //              }
    //            } else {
    //              redirectToBenefitHome
    //            }
    //          }
    //          case _ => redirectToBenefitHome
    //        }
    //        func(request, user)
    //    }

    private def thereAreNoExistingTransactionsMatching(user: User, kind: Int, employmentSequenceNumber: Int, year: Int): Boolean = {
      val transactions = user.regimes.paye.get.recentAcceptedTransactions ++
        user.regimes.paye.get.recentCompletedTransactions
      transactions.find(matchesBenefit(_, kind, employmentSequenceNumber, year)).isEmpty
    }

    private val redirectToBenefitHome: (Request[_], User) => Result = (r, u) => Redirect(routes.BenefitHomeController.listBenefits)
  }
}

