package controllers.paye

import scala.concurrent._

import play.api.mvc._
import play.api.Logger
import play.api.mvc.SimpleResult

import uk.gov.hmrc.common.microservice.paye.domain._
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import uk.gov.hmrc.common.microservice.domain.User

import models.paye._
import views.html.paye._
import controllers.common.actions.HeaderCarrier
import controllers.common.service.Connectors
import controllers.common.{SessionKeys, SessionTimeoutWrapper}
import controllers.paye.validation.RemoveBenefitValidator._

class RemoveBenefitController(keyStoreService: KeyStoreConnector, override val authConnector: AuthConnector, override val auditConnector: AuditConnector)
                             (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector)
  extends BenefitController
  with SessionTimeoutWrapper
  with TaxYearSupport {

  import RemovalUtils._

  def this() = this(Connectors.keyStoreConnector, Connectors.authConnector, Connectors.auditConnector)(Connectors.payeConnector, Connectors.txQueueConnector)

  def requestRemoveCarBenefit(taxYear: Int, employmentSequenceNumber: Int) = benefitController { (user: User, request: Request[_]) =>
    requestRemoveCarBenefitAction(taxYear, employmentSequenceNumber)(user, request)
  }

  def requestRemoveFuelBenefit(taxYear: Int, employmentSequenceNumber: Int) = benefitController {
    (user: User, request: Request[_]) => requestRemoveFuelBenefitAction(taxYear, employmentSequenceNumber)(user, request)
  }

  def confirmCarBenefitRemoval(taxYear: Int, employmentSequenceNumber: Int) = benefitController { (user: User, request: Request[_], version: Int) =>
    confirmCarBenefitRemovalAction(taxYear, employmentSequenceNumber)(user, request, version).removeSessionKey(SessionKeys.npsVersion)(request)
  }

  def confirmFuelBenefitRemoval(taxYear: Int, employmentSequenceNumber: Int) = benefitController { (user: User, request: Request[_]) =>
    confirmFuelBenefitRemovalAction(taxYear, employmentSequenceNumber)(user, request).removeSessionKey(SessionKeys.npsVersion)(request)
  }

  private[paye] def requestRemoveCarBenefitAction(taxYear: Int, employmentSequenceNumber: Int)(implicit user: User, request: Request[_]): Future[SimpleResult] = {
    implicit val hc = HeaderCarrier(request)
    val f1 = user.getPaye.fetchTaxYearData(taxYear)
    val f2 = keyStoreService.loadCarBenefitFormData

    for {
      taxYearData <- f1
      defaults <- f2
      result <- validateRemoveCarBenefitForm(taxYearData, employmentSequenceNumber, defaults)
    } yield result
  }

  private def validateRemoveCarBenefitForm(taxYearData: TaxYearData, employmentSequenceNumber: Int, formData: Option[RemoveCarBenefitFormData])(implicit user: User, request: Request[_]): Future[SimpleResult] = {
    val result = for {
      activeCarBenefit <- taxYearData.findActiveCarBenefit(employmentSequenceNumber)
      primaryEmployment <- taxYearData.findPrimaryEmployment
    } yield {
      val rawData = Some(validationlessForm.bindFromRequest().value.get)
      updateRemoveCarBenefitForm(rawData, activeCarBenefit.dateMadeAvailable, activeCarBenefit.hasActiveFuel, getCarFuelBenefitDates(request), now(), taxYearInterval).bindFromRequest()(request).fold(
        formWithErrors => {
          Future.successful(BadRequest(remove_car_benefit_form(activeCarBenefit, primaryEmployment, formWithErrors, currentTaxYearYearsRange)))
        },
        removeBenefitData => {
          keyStoreService.storeBenefitFormData(removeBenefitData).map { _ =>
            Ok(remove_car_benefit_review(activeCarBenefit, primaryEmployment, removeBenefitData))
          }
        }
      )
    }

    result.getOrElse(Future.successful(InternalServerError("")))
  }


  private[paye] def requestRemoveFuelBenefitAction(taxYear: Int, employmentSequenceNumber: Int)(implicit user: User, request: Request[_]): Future[SimpleResult] = {
    implicit val hc = HeaderCarrier(request)
    val f1 = user.getPaye.fetchTaxYearData(taxYear)
    val f2 = keyStoreService.loadCarBenefitFormData

    for {
      taxYearData <- f1
      defaults <- f2
      result <- validateRemoveFuelBenefitForm(taxYearData, employmentSequenceNumber, defaults)
    } yield result

  }

  def validateRemoveFuelBenefitForm(taxYearData: TaxYearData, employmentSequenceNumber: Int, formData: Option[RemoveCarBenefitFormData])
                                   (implicit user: User, request: Request[_]): Future[SimpleResult] = {
    val result = for {
      activeCarBenefit <- taxYearData.findActiveCarBenefit(employmentSequenceNumber)
      fuelBenefit <- activeCarBenefit.activeFuelBenefit
      primaryEmployment <- taxYearData.findPrimaryEmployment
    } yield {
      updateRemoveFuelBenefitForm(fuelBenefit.startDate, now(), taxYearInterval).bindFromRequest()(request).fold(
        errors => {
          val result = BadRequest(remove_fuel_benefit_form(fuelBenefit, primaryEmployment, activeCarBenefit.taxYear, errors, currentTaxYearYearsRange)(user, request))
          Future.successful(result)
        },
        removeBenefitData => {
          implicit def hc = HeaderCarrier(request)
          keyStoreService.storeBenefitFormData(removeBenefitData).map { _ =>
            Ok(remove_fuel_benefit_review(fuelBenefit, primaryEmployment, activeCarBenefit.taxYear, removeBenefitData)(user, request))
          }
        }
      )
    }
    result.getOrElse(Future.successful(InternalServerError("")))
  }

  private[paye] def confirmCarBenefitRemovalAction(taxYear: Int, employmentSequenceNumber: Int)(user: User, request: Request[_], version: Int): Future[SimpleResult] = {
    implicit val hc = HeaderCarrier(request)

    val f1 = user.getPaye.fetchTaxYearData(taxYear)
    val f2 = keyStoreService.loadCarBenefitFormData

    for {
      taxYearData <- f1
      submissionData <- f2
      result <- doConfirmCarBenefitRemovalAction(version, taxYearData, taxYear, employmentSequenceNumber, submissionData)(user, request, hc)
    } yield result
  }

  private def doConfirmCarBenefitRemovalAction(version: Int, taxYearData: TaxYearData, taxYear: Int, employmentSequenceNumber: Int, submissionDataO: Option[RemoveCarBenefitFormData])
                                              (implicit user: User, request: Request[_], hc: HeaderCarrier): Future[SimpleResult] = {

    val result = for {
      activeCarBenefit <- taxYearData.findActiveCarBenefit(employmentSequenceNumber)
      primaryEmployment <- taxYearData.findPrimaryEmployment
      formData <- submissionDataO
    } yield {
      val uri = activeCarBenefit.actions.getOrElse("remove",
        throw new IllegalArgumentException(s"No remove action uri found for this car benefit."))

      val withdrawnFuelBenefit = activeCarBenefit.activeFuelBenefit.map(_ => WithdrawnFuelBenefit(formData.fuelWithdrawDate.getOrElse(formData.withdrawDate)))

      val withdrawRequest = WithdrawnBenefitRequest(version,
        Some(WithdrawnCarBenefit(formData.withdrawDate,
          formData.numberOfDaysUnavailable,
          formData.removeEmployeeContribution)),
        withdrawnFuelBenefit)

      payeConnector.removeBenefits(uri, withdrawRequest).map(_.get).flatMap { removeBenefitResponse =>
        val benefitTypes = Seq("car") ++ activeCarBenefit.fuelBenefit.map(_=> "fuel")
        renderRemoveBenefitConfirmation(benefitTypes, taxYear, employmentSequenceNumber, removeBenefitResponse.taxCode, removeBenefitResponse.transaction)
      }
    }

    val errorResult = if (submissionDataO.isEmpty) {
      Logger.error(s"Cannot find keystore entry for user ${user.oid}, redirecting to car benefit homepage")
      Redirect(routes.CarBenefitHomeController.carBenefitHome())
    } else InternalServerError("Missing data needed to remove car benefit.")

    result.getOrElse(Future.successful(errorResult))
  }

  private[paye] def confirmFuelBenefitRemovalAction(taxYear: Int, employmentSequenceNumber: Int)(implicit user: User, request: Request[_]): Future[SimpleResult] = {
    implicit val hc = HeaderCarrier(request)
    val f1 = user.getPaye.fetchTaxYearData(taxYear)
    val f2 = keyStoreService.loadFuelBenefitFormData

    for {
      taxYearData <- f1
      submissionData <- f2
      result <- doConfirmFuelBenefitRemovalAction(taxYear, taxYearData, employmentSequenceNumber, submissionData)
    } yield result
  }

  private def doConfirmFuelBenefitRemovalAction(taxYear: Int, taxYearData: TaxYearData, employmentSequenceNumber: Int, submissionDataO: Option[RemoveFuelBenefitFormData])
                                               (implicit user: User, request: Request[_], hc: HeaderCarrier): Future[SimpleResult] = {
    val result = for {
      activeCarBenefit <- taxYearData.findActiveCarBenefit(employmentSequenceNumber)
      fuelBenefit <- activeCarBenefit.activeFuelBenefit
      primaryEmployment <- taxYearData.findPrimaryEmployment
      formData <- submissionDataO
    } yield {
      val uri = fuelBenefit.actions.getOrElse("remove",
        throw new IllegalArgumentException(s"No remove action uri found for this car benefit."))

      val withdrawnFuelBenefit = WithdrawnFuelBenefit(formData.withdrawDate)

      user.getPaye.version.flatMap {
        version =>
          val withdrawRequest = WithdrawnBenefitRequest(version, None, Some(withdrawnFuelBenefit))
          payeConnector.removeBenefits(uri, withdrawRequest).map(_.get).flatMap { removeBenefitResponse =>
            renderRemoveBenefitConfirmation(Seq("fuel"), taxYear, employmentSequenceNumber, removeBenefitResponse.taxCode, removeBenefitResponse.transaction)
          }
      }
    }

    val errorResult = if (submissionDataO.isEmpty) {
      Logger.error(s"Cannot find keystore entry for user ${user.oid}, redirecting to fuel benefit homepage")
      Redirect(routes.CarBenefitHomeController.carBenefitHome())
    } else InternalServerError("Missing data needed to remove fuel benefit.")

    result.getOrElse(Future.successful(errorResult))
  }


  private[paye] def renderRemoveBenefitConfirmation(kinds: Seq[String], year: Int, employmentSequenceNumber: Int, newTaxCode: Option[String], transaction: TransactionId)
                                                   (implicit user: User, request: Request[_], hc: HeaderCarrier): Future[SimpleResult] = {
    keyStoreService.clearBenefitFormData
    TaxCodeResolver.currentTaxCode(user.regimes.paye.get, employmentSequenceNumber, year).map { taxCode =>
      val removalData = BenefitUpdatedConfirmationData(transaction.oid, taxCode, newTaxCode)
      Ok(remove_benefit_confirmation(kinds, removalData))
    }
  }

  private def getCarFuelBenefitDates(request: Request[_]): Option[CarFuelBenefitDates] = {
    datesForm().bindFromRequest()(request).value
  }

}


