package controllers.paye.validation

import play.api.mvc.Request
import models.paye.{DisplayBenefits, DisplayBenefit}
import scala.concurrent._
import ExecutionContext.Implicits.global
import controllers.common.actions.HeaderCarrier
import play.api.Logger
import uk.gov.hmrc.common.microservice.paye.domain.BenefitTypes._
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import uk.gov.hmrc.utils.TaxYearResolver
import controllers.paye.validation.BenefitFlowHelper._
import uk.gov.hmrc.common.microservice.paye.domain.TaxYearData
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User

object RemoveBenefitFlow {
  def apply(action: (User, Request[_], DisplayBenefit, TaxYearData) => Future[SimpleResult])
           (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector, currentTaxYear: Int): (User, Request[_], String, Int, Int) => Future[SimpleResult] =
    (user, request, displayBenefit, taxYear, employmentSequenceNumber) =>
      validateVersionNumber(user, request.session).fold(
        errorResult => Future.successful(errorResult),
        versionNumber =>
          retrieveData(user, request, displayBenefit, taxYear, employmentSequenceNumber).flatMap {
            _.fold(
              errorResult => Future.successful(errorResult),
              data => {
                val (displayBenefit, taxYearData) = data
                action(user, request, displayBenefit, taxYearData)
              }
            )
          }
      )


  def retrieveData(user: User, request: Request[_], benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int)
                  (implicit payeConnector: PayeConnector, txConnector: TxQueueConnector): Future[Either[SimpleResult, (DisplayBenefit, TaxYearData)]] = {

    implicit val hc = HeaderCarrier(request)

    user.regimes.paye.get.fetchTaxYearData(TaxYearResolver.currentTaxYear).map { taxData =>
      val emptyBenefit = DisplayBenefit(null, Seq.empty, None)

      val validBenefits = DisplayBenefit.fromStringAllBenefit(benefitTypes).map {
        kind => getBenefit(kind, taxYear, employmentSequenceNumber, taxData)
      }

      if (!validBenefits.contains(None)) {
        val validMergedBenefit = validBenefits.map(_.get).foldLeft(emptyBenefit)((a: DisplayBenefit, b: DisplayBenefit) =>
          mergeDisplayBenefits(a, b))
        Right((validMergedBenefit, taxData))
      } else {
        Logger.error(s"The requested benefit is not a valid benefit (year: $taxYear, empl: $employmentSequenceNumber, types: $benefitTypes), redirecting to benefit list")
        Left(redirectToCarBenefitHome)
      }
    }
  }

  private def getBenefit(kind: Int, taxYear: Int, employmentSequenceNumber: Int, payeRootData: TaxYearData): Option[DisplayBenefit] = {
    kind match {
      case CAR | FUEL => {
        getBenefitMatching(kind, employmentSequenceNumber, payeRootData)
      }
      case _ => None
    }
  }

  private def getBenefitMatching(kind: Int, employmentSequenceNumber: Int, taxYearData: TaxYearData): Option[DisplayBenefit] = {
    val benefit = taxYearData.findActiveBenefit(employmentSequenceNumber, kind)
    val matchedBenefits = DisplayBenefits(benefit.toList, taxYearData.employments)
    if (matchedBenefits.size > 0) Some(matchedBenefits(0)) else None
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
      employment = if (db1.employment != null) db1.employment else db2.employment
    )
  }


}