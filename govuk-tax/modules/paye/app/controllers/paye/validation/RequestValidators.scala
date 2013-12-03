package controllers.paye.validation

import play.api.mvc.{Request, SimpleResult}
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.domain.{Employment, TaxYearData}
import play.api.Logger
import controllers.paye.routes
import play.api.mvc.Results._
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import controllers.common.actions.HeaderCarrier
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.Try

object Int {
  def unapply(s: String): Option[Int] = Try {s.toInt}.toOption
}

/**
 * A controller wrapper that will check that there is an NPS version number in the play session, and
 * that it corresponds to the version number of the user. If they differ, this indicates that some
 * aspect of the user data was changed during the course of the page flow that was in progress.
 *
 * If the version number is absent from the session, this indicates that the page was accessed
 * directly, outside of the normal flow, or that the session timed out.
 */
private[paye] object WithValidVersionNumber {
  val npsVersionKey: String = "nps-version"

  def apply(benefitType: Int)(action: (Request[_], User, Int, Int, TaxYearData) => Future[SimpleResult])
           (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector, currentTaxYear: Int): (User, Request[_], Int, Int) => Future[SimpleResult] = {

    val noVersion = (user: User, request: Request[_], taxYear: Int, employmentSequenceNumber: Int) => redirectToCarBenefitHome(user, request)
    val versionMismatch = (user: User, request: Request[_], taxYear: Int, employmentSequenceNumber: Int) => redirectToVersionError(user, request)

    (user: User, request: Request[_], taxYear: Int, employmentSequenceNumber: Int) => {
      val sessionVersion = request.session.get(npsVersionKey)

      sessionVersion match {
        case None =>  noVersion(user, request, taxYear, employmentSequenceNumber)
        case Some(Int(version)) => {
          if (version != user.getPaye.version) {
            versionMismatch(user, request, taxYear, employmentSequenceNumber)
          } else {
            val f = WithValidatedRequest.async(benefitType, action)
            f(user, request, taxYear, employmentSequenceNumber)
          }
        }
      }
    }
  }

  private val redirectToCarBenefitHome: (User, Request[_]) => Future[SimpleResult] = (u, r) => Future.successful(Redirect(routes.CarBenefitHomeController.carBenefitHome().url))

  private val redirectToVersionError: (User, Request[_]) => Future[SimpleResult] = (u, r) => Future.successful(Redirect(routes.CarBenefitHomeController.carBenefitHome().url))
}

private object WithValidatedRequest {
  def apply(benefitType: Int, action: (Request[_], User, Int, Int, TaxYearData) => SimpleResult)
           (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector, currentTaxYear: Int): (User, Request[_], Int, Int) => Future[SimpleResult] = {
    (user, request, taxYear, employmentSequenceNumber) => {

      if (currentTaxYear != taxYear) {
        Logger.error("Adding benefit is only allowed for the current tax year")
        Future.successful(BadRequest)
      } else {
        implicit val hc = HeaderCarrier(request)
        user.regimes.paye.get.fetchTaxYearData(currentTaxYear).map { payeRootData =>
          if (employmentSequenceNumber != findPrimaryEmployment(payeRootData).get.sequenceNumber) {
            Logger.error("Adding benefit is only allowed for the primary employment")
            BadRequest
          } else {
            if (payeRootData.findExistingBenefit(employmentSequenceNumber, benefitType).isDefined) {
              redirectToCarBenefitHome(request, user)
            } else {
              action(request, user, taxYear, employmentSequenceNumber, payeRootData)
            }
          }
        }
      }
    }
  }

  def async(benefitType: Int, action: (Request[_], User, Int, Int, TaxYearData) => Future[SimpleResult])
           (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector, currentTaxYear: Int): (User, Request[_], Int, Int) => Future[SimpleResult] = {
    (user, request, taxYear, employmentSequenceNumber) => {

      if (currentTaxYear != taxYear) {
        Logger.error("Adding fuel benefit is only allowed for the current tax year")
        Future.successful(BadRequest)
      } else {
        implicit val hc = HeaderCarrier(request)
        user.regimes.paye.get.fetchTaxYearData(currentTaxYear).flatMap { payeRootData =>
          if (employmentSequenceNumber != findPrimaryEmployment(payeRootData).get.sequenceNumber) {
            Logger.error("Adding fuel benefit is only allowed for the primary employment")
            Future.successful(BadRequest)
          } else {
            if (payeRootData.findExistingBenefit(employmentSequenceNumber, benefitType).isDefined) {
              Future.successful(redirectToCarBenefitHome(request, user))
            } else {
              action(request, user, taxYear, employmentSequenceNumber, payeRootData)
            }
          }
        }
      }
    }
  }

  private def findPrimaryEmployment(payeRootData: TaxYearData): Option[Employment] = payeRootData.employments.find(_.employmentType == primaryEmploymentType)

  private val redirectToCarBenefitHome: (Request[_], User) => SimpleResult = (r, u) =>
    Redirect(routes.CarBenefitHomeController.carBenefitHome().url)

  private val redirectToVersionError: (Request[_], User) => SimpleResult = (r, u) => Redirect(routes.CarBenefitHomeController.carBenefitHome().url)
}





