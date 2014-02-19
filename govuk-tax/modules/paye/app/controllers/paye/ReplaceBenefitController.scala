package controllers.paye

import scala.concurrent._

import play.api.mvc._
import play.api.data.Form
import play.api.mvc.SimpleResult

import uk.gov.hmrc.utils.TaxYearResolver

import uk.gov.hmrc.common.microservice.paye.domain._
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.domain.TaxYearData

import models.paye._

import views.html.paye._
import views.formatting.Strings

import controllers.common.actions.{HeaderCarrier, Actions}
import controllers.paye.validation.RemoveBenefitValidator.RemoveCarBenefitFormDataValues
import controllers.paye.validation.BenefitFlowHelper._
import controllers.paye.validation.RemoveBenefitValidator
import controllers.paye.validation.AddCarBenefitValidator._
import controllers.common.{BaseController, SessionTimeoutWrapper}
import controllers.common.service.Connectors

class ReplaceBenefitController(keyStoreService: KeyStoreConnector, override val authConnector: AuthConnector, override val auditConnector: AuditConnector)
                              (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector)
  extends BenefitController
  with SessionTimeoutWrapper
  with TaxYearSupport {

  import RemovalUtils._

  def this() = this(Connectors.keyStoreConnector, Connectors.authConnector, Connectors.auditConnector)(Connectors.payeConnector, Connectors.txQueueConnector)

  def showReplaceCarBenefitForm(taxYear: Int, employmentSequenceNumber: Int) = benefitController { (user: User, request: Request[_]) =>
    showReplaceCarBenefitFormAction(taxYear, employmentSequenceNumber)(user, request)
  }

  def requestReplaceCarBenefit(taxYear: Int, employmentSequenceNumber: Int) = benefitController { (user: User, request: Request[_]) =>
    requestReplaceCarAction(taxYear, employmentSequenceNumber)(user, request)
  }

  private[paye] def replaceCarBenefit(activeCarBenefit: CarBenefit, primaryEmployment: Employment, dates: CarFuelBenefitDates, removeDefaults: Option[RemoveCarBenefitFormData], addDefaults: Form[CarBenefitData], user: User, request: Request[_]) = {
    val benefitValues: Option[RemoveCarBenefitFormDataValues] = removeDefaults.map(RemoveCarBenefitFormDataValues(_))
    val benefitForm: Form[RemoveCarBenefitFormData] = updateRemoveCarBenefitForm(benefitValues, activeCarBenefit.startDate, activeCarBenefit.fuelBenefit, dates, now(), taxYearInterval)
    val filledForm = removeDefaults.map {
      preFill => benefitForm.fill(preFill)
    }.getOrElse(benefitForm)

    replace_car_benefit_form(activeCarBenefit, primaryEmployment, filledForm, addDefaults, currentTaxYearYearsRange)(user, request)
  }


  private[paye] def showReplaceCarBenefitFormAction(taxYear: Int, employmentSequenceNumber: Int)
                                                   (implicit user: User, request: Request[_]): Future[SimpleResult] = {
    val f1 = user.getPaye.fetchTaxYearData(currentTaxYear)
    val f2 = keyStoreService.loadFormData
    for {
      taxYearData <- f1
      defaults <- f2
    } yield {
      for {
        activeCarBenefit <- taxYearData.findActiveCarBenefit(employmentSequenceNumber)
        primaryEmployment <- taxYearData.findPrimaryEmployment
      } yield {
        val removeFormData = defaults.map(_.removedCar)
        val addFormData = extractCarBenefitValuesAndBuildForm(defaults.map(_.newCar))
        Ok(replaceCarBenefit(activeCarBenefit, primaryEmployment, getDatesFromDefaults(removeFormData), removeFormData, addFormData, user, request))
      }
    }.getOrElse(Redirect(routes.CarBenefitHomeController.carBenefitHome()))
  }

  private[paye] def requestReplaceCarAction(taxYear: Int, employmentSequenceNumber: Int)(implicit user: User, request: Request[_]): Future[SimpleResult] = {
    implicit val hc = HeaderCarrier(request)
    user.getPaye.fetchTaxYearData(taxYear).flatMap(renderReplaceCarBenefitSummary(_, employmentSequenceNumber))
  }


  private def renderReplaceCarBenefitSummary(taxYearData: TaxYearData, employmentSequenceNumber: Int)(implicit user: User, request: Request[_]): Future[SimpleResult] = {
    val result = for {
      activeCarBenefit <- taxYearData.findActiveCarBenefit(employmentSequenceNumber)
      primaryEmployment <- taxYearData.findPrimaryEmployment
    } yield {
      val rawData = Some(RemoveBenefitValidator.validationlessForm.bindFromRequest().value.get)
      val removeForm = updateRemoveCarBenefitForm(rawData, activeCarBenefit.dateMadeAvailable, activeCarBenefit.fuelBenefit, getCarFuelBenefitDates(request), now(), taxYearInterval).bindFromRequest()

      val dates = getCarBenefitDates(request)
      val addForm = carBenefitForm(dates).bindFromRequest()

      if (addForm.hasErrors || removeForm.hasErrors) {
        Future.successful(BadRequest(replace_car_benefit_form(activeCarBenefit, primaryEmployment, removeForm, addForm, currentTaxYearYearsRange)))
      } else {
        val addCarBenefitData = addForm.get

        import AddCarBenefitConfirmationData._

        val confirmationData = AddCarBenefitConfirmationData(Strings.optionalValue(primaryEmployment.employerName, "your.employer"), addCarBenefitData.providedFrom.getOrElse(startOfCurrentTaxYear),
          addCarBenefitData.listPrice.get, addCarBenefitData.fuelType.get, addCarBenefitData.co2Figure, addCarBenefitData.engineCapacity,
          convertEmployerPayFuel(addCarBenefitData.fuelType, addCarBenefitData.employerPayFuel), addCarBenefitData.employeeContribution,
          addCarBenefitData.carRegistrationDate, addCarBenefitData.privateUsePaymentAmount)

        keyStoreService.storeFormData(ReplaceCarBenefitFormData(removeForm.get, addCarBenefitData)).map { _ =>
          Ok(replace_car_benefit_review(activeCarBenefit, primaryEmployment, removeForm.get, confirmationData))
        }
      }
    }

    result.getOrElse(Future.successful(InternalServerError("")))
  }


  private def extractCarBenefitValuesAndBuildForm(carBenefitDataO: Option[CarBenefitData])(implicit hc: HeaderCarrier): Form[CarBenefitData] = {
    carBenefitDataO.map { carBenefitData =>
      val rawForm = validationlessForm
      val valuesForValidation = rawForm.fill(rawValuesOf(carBenefitData)).value.get
      val form = carBenefitForm(valuesForValidation, timeSource).fill(carBenefitData)
      form
    }.getOrElse(carBenefitForm(CarBenefitValues(), timeSource))
  }


  private[paye] def rawValuesOf(defaults: CarBenefitData) =
    CarBenefitValues(providedFromVal = defaults.providedFrom,
      carRegistrationDate = defaults.carRegistrationDate,
      employeeContributes = defaults.employeeContributes.map(_.toString),
      privateUsePayment = defaults.privateUsePayment.map(_.toString),
      fuelType = defaults.fuelType,
      co2Figure = defaults.co2Figure.map(_.toString),
      co2NoFigure = defaults.co2NoFigure.map(_.toString),
      employerPayFuel = defaults.employerPayFuel)

}



