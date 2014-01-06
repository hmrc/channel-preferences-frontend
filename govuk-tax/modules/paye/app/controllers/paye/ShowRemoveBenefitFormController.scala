package controllers.paye

import scala.concurrent._
import play.api.mvc._

import config.DateTimeProvider
import controllers.common.{SessionTimeoutWrapper, BaseController}
import controllers.common.actions.{HeaderCarrier, Actions}
import controllers.common.service.Connectors

import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import uk.gov.hmrc.common.microservice.paye.domain._

import models.paye.{RemoveFuelBenefitFormData, RemoveCarBenefitFormData}
import views.html.paye.{remove_fuel_benefit_form, remove_car_benefit_form}
import play.api.data.Form
import controllers.paye.validation.RemoveBenefitValidator.RemoveCarBenefitFormDataValues
import controllers.paye.validation.BenefitFlowHelper._
import models.paye.CarFuelBenefitDates
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.utils.TaxYearResolver

class ShowRemoveBenefitFormController(keyStoreService: KeyStoreConnector, override val authConnector: AuthConnector, override val auditConnector: AuditConnector)
                                     (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector)
  extends BaseController
  with Actions
  with SessionTimeoutWrapper
  with TaxYearSupport
  with DateTimeProvider
  with PayeRegimeRoots {

  def this() = this(Connectors.keyStoreConnector, Connectors.authConnector, Connectors.auditConnector)(Connectors.payeConnector, Connectors.txQueueConnector)

  import RemovalUtils._

  def showRemoveCarBenefitForm(taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(PayeRegime).async {
    user =>
      request =>
        implicit val hc = HeaderCarrier(request)
        validateVersionNumber(user, request.session).flatMap {
          _.fold(
            errorResult => Future.successful(errorResult),
            versionNumber => showRemoveCarBenefitFormAction(user, request, taxYear, employmentSequenceNumber))
        }
  }

  def showRemoveFuelBenefitForm(taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(PayeRegime).async {
    user =>
      request =>
        implicit val hc = HeaderCarrier(request)
        validateVersionNumber(user, request.session).flatMap {
          _.fold(
            errorResult => Future.successful(errorResult),
            versionNumber => showRemoveFuelBenefitFormAction(user, request, taxYear, employmentSequenceNumber))
        }
  }

  private[paye] def showRemoveCarBenefitFormAction(user: User, request: Request[_], taxYear: Int, employmentSequenceNumber: Int): Future[SimpleResult] = {
    implicit val hc = HeaderCarrier(request)

    val f1 = user.getPaye.fetchTaxYearData(TaxYearResolver.currentTaxYear)
    val f2 = keyStoreService.loadCarBenefitFormData

    for {
      taxYearData <- f1
      defaults <- f2
    } yield {
      for {
        activeCarBenefit <- taxYearData.findActiveCarBenefit(employmentSequenceNumber)
        primaryEmployment <- taxYearData.findPrimaryEmployment
      } yield {
        Ok(removeCarBenefit(activeCarBenefit, primaryEmployment, getDatesFromDefaults(defaults), defaults, user, request))
      }
    }.getOrElse(Redirect(routes.CarBenefitHomeController.carBenefitHome()))
  }

  def removeCarBenefit(activeCarBenefit: CarBenefit, primaryEmployment: Employment, dates: Option[CarFuelBenefitDates], defaults: Option[RemoveCarBenefitFormData], user: User, request: Request[_]) = {
    val hasUnremovedFuel = activeCarBenefit.hasActiveFuel
    val benefitValues: Option[RemoveCarBenefitFormDataValues] = defaults.map(RemoveCarBenefitFormDataValues(_))
    val benefitForm: Form[RemoveCarBenefitFormData] = updateRemoveCarBenefitForm(benefitValues, activeCarBenefit.startDate, hasUnremovedFuel, dates, now(), taxYearInterval)
    val filledForm = defaults.map {
      preFill => benefitForm.fill(preFill)
    }.getOrElse(benefitForm)

    remove_car_benefit_form(activeCarBenefit, primaryEmployment, filledForm, currentTaxYearYearsRange)(user, request)
  }

  private[paye] def showRemoveFuelBenefitFormAction(user: User, request: Request[_], taxYear: Int, employmentSequenceNumber: Int): Future[SimpleResult] = {
    implicit val hc = HeaderCarrier(request)
    val f1 = user.getPaye.fetchTaxYearData(TaxYearResolver.currentTaxYear)
    val f2 = keyStoreService.loadFuelBenefitFormData

    for {
      taxYearData <- f1
      defaults <- f2
    } yield {
      for {
        activeFuelBenefit <- taxYearData.findActiveFuelBenefit(employmentSequenceNumber)
        primaryEmployment <- taxYearData.findPrimaryEmployment
      } yield {
        Ok(removeFuelBenefit(activeFuelBenefit, primaryEmployment, taxYear, defaults, user, request))
      }
    }.getOrElse(Redirect(routes.CarBenefitHomeController.carBenefitHome()))

  }

  def removeFuelBenefit(activeFuelBenefit: FuelBenefit, primaryEmployment: Employment, taxYear: Int, defaults: Option[RemoveFuelBenefitFormData], user: User, request : Request[_]) = {
    val benefitForm: Form[RemoveFuelBenefitFormData] = updateRemoveFuelBenefitForm(activeFuelBenefit.startDate, now(), taxYearInterval)
    val filledForm = defaults.map {
      preFill => benefitForm.fill(preFill)
    }.getOrElse(benefitForm)
    remove_fuel_benefit_form(activeFuelBenefit, primaryEmployment, taxYear, filledForm, currentTaxYearYearsRange)(user, request)
  }
}
