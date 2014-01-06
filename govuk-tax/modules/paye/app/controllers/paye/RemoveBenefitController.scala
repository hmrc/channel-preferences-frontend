package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain._
import play.api.mvc._
import views.html.paye._
import org.joda.time.LocalDate
import models.paye._
import controllers.common.{BaseController, SessionTimeoutWrapper}
import controllers.paye.validation.RemoveBenefitValidator._
import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import controllers.common.service.Connectors
import play.api.Logger
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import controllers.common.actions.{HeaderCarrier, Actions}
import BenefitTypes._
import scala.concurrent._
import controllers.paye.validation.BenefitFlowHelper
import scala.Some
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import models.paye.BenefitUpdatedConfirmationData
import uk.gov.hmrc.common.microservice.paye.domain.WithdrawnFuelBenefit
import uk.gov.hmrc.common.microservice.paye.domain.WithdrawnBenefitRequest
import uk.gov.hmrc.common.microservice.paye.domain.WithdrawnCarBenefit
import models.paye.CarFuelBenefitDates
import uk.gov.hmrc.common.microservice.paye.domain.TaxYearData
import controllers.paye.validation.BenefitFlowHelper._
import scala.Some
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import models.paye.BenefitUpdatedConfirmationData
import uk.gov.hmrc.common.microservice.paye.domain.WithdrawnFuelBenefit
import uk.gov.hmrc.common.microservice.paye.domain.WithdrawnBenefitRequest
import uk.gov.hmrc.common.microservice.paye.domain.WithdrawnCarBenefit
import models.paye.CarFuelBenefitDates
import uk.gov.hmrc.common.microservice.paye.domain.TaxYearData

class RemoveBenefitController(keyStoreService: KeyStoreConnector, override val authConnector: AuthConnector, override val auditConnector: AuditConnector)
                             (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector)
  extends BaseController
  with Actions
  with SessionTimeoutWrapper
  with TaxYearSupport
  with PayeRegimeRoots {

  import RemovalUtils._

  def this() = this(Connectors.keyStoreConnector, Connectors.authConnector, Connectors.auditConnector)(Connectors.payeConnector, Connectors.txQueueConnector)


  def requestRemoveCarBenefit(taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(PayeRegime).async {
    implicit user =>
      implicit request =>
        validateVersionNumber(user, request.session).flatMap { _.fold(
          errorResult => Future.successful(errorResult),
          versionNumber => requestRemoveCarBenefitAction(taxYear, employmentSequenceNumber))
        }
  }

  def requestRemoveFuelBenefit(taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(PayeRegime).async {
    implicit user =>
      implicit request =>
        validateVersionNumber(user, request.session).flatMap { _.fold(
          errorResult => Future.successful(errorResult),
          versionNumber => requestRemoveFuelBenefitAction(taxYear, employmentSequenceNumber))
        }
  }

  def confirmCarBenefitRemoval(taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(PayeRegime).async {
    implicit user =>
      implicit request =>
        implicit val hc = HeaderCarrier(request)
        validateVersionNumber(user, request.session).flatMap { _.fold(
          errorResult => Future.successful(errorResult),
          versionNumber => confirmCarBenefitRemovalAction(taxYear, employmentSequenceNumber))
        }
  }

  def confirmFuelBenefitRemoval(taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(PayeRegime).async {
    implicit user =>
      implicit request =>
       implicit val hc = HeaderCarrier(request)
        validateVersionNumber(user, request.session).flatMap { _.fold(
          errorResult => Future.successful(errorResult),
          versionNumber => confirmFuelBenefitRemovalAction(taxYear, employmentSequenceNumber))
        }
  }

  def benefitRemoved(benefitTypes: String, year: Int, employmentSequenceNumber: Int, oid: String, newTaxCode: Option[String], personalAllowance: Option[Int]) =
    AuthorisedFor(PayeRegime).async {
      user =>
        implicit request =>
          benefitRemovedAction(user, request, benefitTypes, year, employmentSequenceNumber, oid, newTaxCode, personalAllowance).removeSessionKey(BenefitFlowHelper.npsVersionKey)
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
      updateRemoveCarBenefitForm(rawData, activeCarBenefit.startDate, activeCarBenefit.hasActiveFuel, getCarFuelBenefitDates(request), now(), taxYearInterval).bindFromRequest()(request).fold(
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
          val result = BadRequest(remove_fuel_benefit_form(fuelBenefit, primaryEmployment, activeCarBenefit.taxYear, errors, currentTaxYearYearsRange)(user))
          Future.successful(result)
        },
        removeBenefitData => {
          implicit def hc = HeaderCarrier(request)
          keyStoreService.storeBenefitFormData(removeBenefitData).map { _ =>
            Ok(remove_fuel_benefit_review(fuelBenefit, primaryEmployment, activeCarBenefit.taxYear, removeBenefitData)(user))
          }
        }
      )
    }
    result.getOrElse(Future.successful(InternalServerError("")))
  }

  private[paye] def confirmCarBenefitRemovalAction(taxYear: Int, employmentSequenceNumber: Int)(implicit user: User, request: Request[_]): Future[SimpleResult] = {
    implicit val hc = HeaderCarrier(request)
    val f1 = user.getPaye.fetchTaxYearData(taxYear)
    val f2 = keyStoreService.loadCarBenefitFormData

    for {
      taxYearData <- f1
      submissionData <- f2
      result <- doConfirmCarBenefitRemovalAction(taxYearData, employmentSequenceNumber, submissionData)
    } yield result
  }

  private def doConfirmCarBenefitRemovalAction(taxYearData: TaxYearData, employmentSequenceNumber: Int, submissionDataO: Option[RemoveCarBenefitFormData])
                                              (implicit user: User, request: Request[_], hc: HeaderCarrier): Future[SimpleResult] = {
    val result = for {
      activeCarBenefit <- taxYearData.findActiveCarBenefit(employmentSequenceNumber)
      primaryEmployment <- taxYearData.findPrimaryEmployment
      formData <- submissionDataO
    } yield {

      user.getPaye.version.flatMap {
        version =>

        val uri = activeCarBenefit.actions.getOrElse("remove",
          throw new IllegalArgumentException(s"No remove action uri found for this car benefit."))

        val withdrawnFuelBenefit = activeCarBenefit.activeFuelBenefit.map(_ => WithdrawnFuelBenefit(formData.fuelWithdrawDate.getOrElse(formData.withdrawDate)))

        val request = WithdrawnBenefitRequest(version,
          Some(WithdrawnCarBenefit(formData.withdrawDate,
            formData.numberOfDaysUnavailable,
            formData.employeeContribution)),
          withdrawnFuelBenefit)

        payeConnector.removeBenefits(uri, request).map(_.get).map { removeBenefitResponse =>
          keyStoreService.clearBenefitFormData

          Redirect(routes.RemoveBenefitController.benefitRemoved(activeCarBenefit.benefitCode.toString,
            activeCarBenefit.taxYear, employmentSequenceNumber, removeBenefitResponse.transaction.oid,
            removeBenefitResponse.calculatedTaxCode, removeBenefitResponse.personalAllowance))
        }
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
      result <- doConfirmFuelBenefitRemovalAction(taxYearData, employmentSequenceNumber, submissionData)
    } yield result
  }

  private def doConfirmFuelBenefitRemovalAction(taxYearData: TaxYearData, employmentSequenceNumber: Int, submissionDataO: Option[RemoveFuelBenefitFormData])
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

      user.getPaye.version.flatMap{
        version =>

        val withdrawRequest = WithdrawnBenefitRequest(version, None, Some(withdrawnFuelBenefit))

        payeConnector.removeBenefits(uri, withdrawRequest).map(_.get).map { removeBenefitResponse =>
            keyStoreService.clearBenefitFormData

            Redirect(routes.RemoveBenefitController.benefitRemoved(fuelBenefit.benefitCode.toString,
            activeCarBenefit.taxYear, employmentSequenceNumber, removeBenefitResponse.transaction.oid,
            removeBenefitResponse.calculatedTaxCode, removeBenefitResponse.personalAllowance))
        }
      }
    }

    val errorResult = if (submissionDataO.isEmpty) {
      Logger.error(s"Cannot find keystore entry for user ${user.oid}, redirecting to fuel benefit homepage")
      Redirect(routes.CarBenefitHomeController.carBenefitHome())
    } else InternalServerError("Missing data needed to remove fuel benefit.")

    result.getOrElse(Future.successful(errorResult))
  }


  // TODO: Convert this away from using the "kinds" parameter
  private[paye] val benefitRemovedAction: (User, Request[_], String, Int, Int, String, Option[String], Option[Int]) =>
    Future[SimpleResult] = (user, request, kinds, year, employmentSequenceNumber, oid, newTaxCode, personalAllowance) => {
    implicit def hc = HeaderCarrier(request)

    txQueueConnector.transaction(oid, user.regimes.paye.get).flatMap {
      case None => Future.successful(NotFound)
      case Some(tx) => {
        keyStoreService.clearBenefitFormData
        TaxCodeResolver.currentTaxCode(user.regimes.paye.get, employmentSequenceNumber, year).map { taxCode =>
          val removalData = BenefitUpdatedConfirmationData(taxCode, newTaxCode, personalAllowance, startOfCurrentTaxYear, endOfCurrentTaxYear)
          Ok(remove_benefit_confirmation(kinds.split(",").map(_.toInt), removalData)(user))
        }
      }
    }
  }

  private def getCarFuelBenefitDates(request: Request[_]): Option[CarFuelBenefitDates] = {
    datesForm().bindFromRequest()(request).value
  }

}


