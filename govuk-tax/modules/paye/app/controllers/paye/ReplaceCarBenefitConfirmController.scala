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

import controllers.common.actions.{Actions, HeaderCarrier}
import controllers.paye.validation.BenefitFlowHelper._
import controllers.common.{SessionTimeoutWrapper, BaseController}
import controllers.common.service.Connectors


class ReplaceCarBenefitConfirmController(keyStoreService: KeyStoreConnector, override val authConnector: AuthConnector, override val auditConnector: AuditConnector)
                                        (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector)
  extends BaseController
  with Actions
  with SessionTimeoutWrapper
  with PayeRegimeRoots {

  def this() = this(Connectors.keyStoreConnector, Connectors.authConnector, Connectors.auditConnector)(Connectors.payeConnector, Connectors.txQueueConnector)

  import RemovalUtils.ReplaceBenefitKeyStore

  def confirmCarBenefitReplacement(taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(PayeRegime).async {
    implicit user =>
      implicit request =>
        implicit val hc = HeaderCarrier(request)
        validateVersionNumber(user, request.session).flatMap {
          _.fold(
            errorResult => Future.successful(errorResult),
            versionNumber => confirmCarBenefitReplacementAction(taxYear, employmentSequenceNumber, versionNumber))
        }
  }

  private[paye] def confirmCarBenefitReplacementAction(taxYear: Int, employmentSequenceNumber: Int, version: Int)(implicit user: User, request: Request[_]): Future[SimpleResult] = {
    val taxYeadDataF = user.getPaye.fetchTaxYearData(TaxYearResolver.currentTaxYear)
    val taxCodeF = TaxCodeResolver.currentTaxCode(user.regimes.paye.get, employmentSequenceNumber, taxYear)
    val formDataF = keyStoreService.loadFormData

    for {
      currentTaxCode <- taxCodeF
      taxYearData <- taxYeadDataF
      formDataO <- formDataF
      result <- withCarBenefitAndFormData(taxYearData.findActiveCarBenefit(employmentSequenceNumber), formDataO) {
        doUpdate(version, taxYear, employmentSequenceNumber, currentTaxCode, user)
      }.fold(err => Future.successful(InternalServerError(err)), r => r)
    } yield result
  }


  private def doUpdate(version: Int, taxYear: Int, employmentSequenceNumber: Int, currentTaxCode: String, user: User)
              (implicit hc:HeaderCarrier): (CarBenefit, ReplaceCarBenefitFormData) => Future[SimpleResult] = {
    (activeCarBenefit, formData) =>
      val url = activeCarBenefit.actions.getOrElse("replace", throw new IllegalArgumentException(s"No replace action uri found for this car benefit."))

      payeConnector.replaceBenefits(url, buildRequest(version, formData, taxYear, employmentSequenceNumber)).map {
        case Some(response) => {
          keyStoreService.clearFormData
          Ok(replace_benefit_confirmation(currentTaxCode, response.taxCode)(user))
        }
        case None => InternalServerError("Got no response back from microservice call to replace benefits")
      }
  }

  private def withCarBenefitAndFormData(carBenefitO: Option[CarBenefit], formDataO: Option[ReplaceCarBenefitFormData])
                                       (body: (CarBenefit, ReplaceCarBenefitFormData) => Future[SimpleResult]): Either[String, Future[SimpleResult]] = {
    (carBenefitO, formDataO) match {
      case (None, None) => Left("Could not find an active car benefit and form data")
      case (None, Some(_)) => Left("Could not find an active car benefit")
      case (Some(_), None) => Left("Could not find form data")
      case (Some(carBenefit), Some(formData)) => Right(body(carBenefit, formData))
    }
  }

  private def buildRequest(version: Int, formData: ReplaceCarBenefitFormData, taxYear: Int, employmentSequenceNumber: Int) = {
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
