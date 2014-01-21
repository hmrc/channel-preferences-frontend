package controllers.paye

import scala.concurrent.Future

import play.api.mvc.Request
import play.api.mvc.SimpleResult

import uk.gov.hmrc.utils.TaxYearResolver

import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.auth._
import uk.gov.hmrc.common.microservice.paye.domain._
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector

import models.paye._
import views.html.paye.replace_benefit_confirmation

import controllers.common.actions.HeaderCarrier
import controllers.common.service.Connectors

class ReplaceCarBenefitConfirmController(keyStoreService: KeyStoreConnector, override val authConnector: AuthConnector, override val auditConnector: AuditConnector)
                                        (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector)
  extends BenefitController {
  def this() = this(Connectors.keyStoreConnector, Connectors.authConnector, Connectors.auditConnector)(Connectors.payeConnector, Connectors.txQueueConnector)

  import RemovalUtils.ReplaceBenefitKeyStore

  def confirmCarBenefitReplacement(taxYear: Int, employmentSequenceNumber: Int) = benefitController { (user: User, request: Request[_], version: Int) =>
    confirmCarBenefitReplacementAction(taxYear, employmentSequenceNumber, version)(user, request)
  }

  import ReplaceCarBenefitConfirmController._

  private[paye] def confirmCarBenefitReplacementAction(taxYear: Int, employmentSequenceNumber: Int, version: Int)(implicit user: User, request: Request[_]): Future[SimpleResult] = {
    val taxYeadDataF = user.getPaye.fetchTaxYearData(TaxYearResolver.currentTaxYear)
    val taxCodeF = TaxCodeResolver.currentTaxCode(user.regimes.paye.get, employmentSequenceNumber, taxYear)
    val formDataF = keyStoreService.loadFormData

    for {
      currentTaxCode <- taxCodeF
      taxYearData <- taxYeadDataF
      formDataO <- formDataF
      result <- withCarBenefitAndFormData(taxYearData.findActiveCarBenefit(employmentSequenceNumber), formDataO) {
        buildUpdateFunction(version, taxYear, employmentSequenceNumber, currentTaxCode, user, request)
      }.fold(err => Future.successful(InternalServerError(err + s" version=$version, taxYear=$taxYear, employmentSequenceNumber=$employmentSequenceNumber")), r => r)
    } yield result
  }

  private def buildUpdateFunction(version: Int, taxYear: Int, employmentSequenceNumber: Int, currentTaxCode: String, user: User, request: Request[_])
                                 (implicit hc: HeaderCarrier): UpdateFunction = {
    (activeCarBenefit, formData) =>
      val url = activeCarBenefit.actions.getOrElse("replace", throw new IllegalArgumentException(s"No replace action uri found for this car benefit."))

      payeConnector.replaceBenefits(url, buildRequest(version, formData, taxYear, employmentSequenceNumber)).map {
        case Some(response) => {
          keyStoreService.clearFormData
          Ok(replace_benefit_confirmation(response.transaction.oid, currentTaxCode, response.taxCode)(user, request))
        }
        case None => InternalServerError("Got no response back from microservice call to replace benefits")
      }
  }
}

object ReplaceCarBenefitConfirmController {
  type UpdateFunction = (CarBenefit, ReplaceCarBenefitFormData) => Future[SimpleResult]

  def withCarBenefitAndFormData(carBenefitO: Option[CarBenefit], formDataO: Option[ReplaceCarBenefitFormData])
                               (body: UpdateFunction): Either[String, Future[SimpleResult]] = {
    (carBenefitO, formDataO) match {
      case (None, None) => Left("Could not find an active car benefit and form data")
      case (None, Some(_)) => Left("Could not find an active car benefit")
      case (Some(_), None) => Left("Could not find form data")
      case (Some(carBenefit), Some(formData)) => Right(body(carBenefit, formData))
    }
  }

  def buildRequest(version: Int, formData: ReplaceCarBenefitFormData, taxYear: Int, employmentSequenceNumber: Int) = {
    val wbr = WithdrawnBenefitRequest(version,
      Some(WithdrawnCarBenefit(formData.removedCar.withdrawDate,
        formData.removedCar.numberOfDaysUnavailable,
        formData.removedCar.removeEmployeeContribution)),
      Some(WithdrawnFuelBenefit(formData.removedCar.withdrawDate)))

    val addBenefits = CarBenefitBuilder(formData.newCar, taxYear, employmentSequenceNumber).toBenefits
    val addBenefit = AddBenefit(version, employmentSequenceNumber, addBenefits)
    ReplaceBenefit(wbr, addBenefit)
  }
}
