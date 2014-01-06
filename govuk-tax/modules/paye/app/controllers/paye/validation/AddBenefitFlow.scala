package controllers.paye.validation

import play.api.mvc.{Request, SimpleResult}
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.Try
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.domain.{TaxYearData, Employment}
import Employment._
import play.api.Logger
import controllers.common.actions.HeaderCarrier
import play.api.mvc.Results._
import play.api.mvc.Session
import controllers.paye.routes


object AddBenefitFlow {

  import BenefitFlowHelper._

  def apply(benefitType: Int)(action: (User, Request[_], Int, Int, TaxYearData) => Future[SimpleResult])
           (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector, currentTaxYear: Int):
  (User, Request[_], Int, Int) => Future[SimpleResult] = {
    (user, request, taxYear, employmentSequenceNumber) =>
      implicit val hc = HeaderCarrier(request)
      validateVersionNumber(user, request.session).flatMap {
        _.fold(
          failureResult => Future.successful(failureResult),
          versionNumber =>
            retrieveTaxYearData(user, request, benefitType, taxYear, employmentSequenceNumber).flatMap {
              result =>
                result.fold(
                  failureResult => Future.successful(failureResult),
                  payeRootData => action(user, request, taxYear, employmentSequenceNumber, payeRootData)
                )
            })
      }
  }


  def retrieveTaxYearData(user: User, request: Request[_], benefitType: Int, taxYear: Int, employmentSequenceNumber: Int)
                         (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector, currentTaxYear: Int):
  Future[Either[SimpleResult, TaxYearData]] = {
    if (currentTaxYear != taxYear) {
      Logger.error("Adding fuel benefit is only allowed for the current tax year")
      Future.successful(Left(BadRequest))
    } else {
      implicit val hc = HeaderCarrier(request)
      user.regimes.paye.get.fetchTaxYearData(currentTaxYear).map {
        payeRootData =>
          if (employmentSequenceNumber != findPrimaryEmployment(payeRootData).get.sequenceNumber) {
            Logger.error("Adding fuel benefit is only allowed for the primary employment")
            Left(BadRequest)
          } else {
            if (payeRootData.hasActiveBenefit(employmentSequenceNumber, benefitType)) {
              Left(redirectToCarBenefitHome)
            } else {
              Right(payeRootData)
            }
          }
      }
    }
  }


  private def findPrimaryEmployment(payeRootData: TaxYearData): Option[Employment] =
    payeRootData.employments.find(_.employmentType == primaryEmploymentType)
}








