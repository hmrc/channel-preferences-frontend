package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain._
import play.api.mvc._
import views.html.paye._
import models.paye._
import controllers.common.{BaseController, SessionTimeoutWrapper}
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import controllers.common.actions.{HeaderCarrier, Actions}
import scala.concurrent._
import controllers.paye.validation.RemoveBenefitValidator
import controllers.paye.validation.BenefitFlowHelper._
import controllers.paye.validation.AddCarBenefitValidator._
import play.api.data.Form
import scala.Some
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.domain.TaxYearData
import controllers.paye.validation.AddCarBenefitValidator.CarBenefitValues
import uk.gov.hmrc.utils.TaxYearResolver
import controllers.paye.validation.RemoveBenefitValidator.RemoveCarBenefitFormDataValues
import views.formatting.Strings

class ReplaceBenefitController(keyStoreService: KeyStoreConnector, override val authConnector: AuthConnector, override val auditConnector: AuditConnector)
                              (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector)
  extends BaseController
  with Actions
  with SessionTimeoutWrapper
  with PayeRegimeRoots {

  import RemovalUtils._

  def this() = this(Connectors.keyStoreConnector, Connectors.authConnector, Connectors.auditConnector)(Connectors.payeConnector, Connectors.txQueueConnector)

  def showReplaceCarBenefitForm(taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(PayeRegime).async {
    implicit user =>
      implicit request =>
        implicit val hc = HeaderCarrier(request)
        validateVersionNumber(user, request.session).flatMap {
          _.fold(
            errorResult => Future.successful(errorResult),
            versionNumber => showReplaceCarBenefitFormAction(user, request, taxYear, employmentSequenceNumber))
        }
  }

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

  def replaceCarBenefit(activeCarBenefit: CarBenefit, primaryEmployment: Employment, dates: Option[CarFuelBenefitDates], removeDefaults: Option[RemoveCarBenefitFormData], addDefaults: Form[CarBenefitData], user: User) = {
    val hasUnremovedFuel = activeCarBenefit.hasActiveFuel
    val benefitValues: Option[RemoveCarBenefitFormDataValues] = removeDefaults.map(RemoveCarBenefitFormDataValues(_))
    val benefitForm: Form[RemoveCarBenefitFormData] = updateRemoveCarBenefitForm(benefitValues, activeCarBenefit.startDate, hasUnremovedFuel, dates, now(), taxYearInterval)
    val filledForm = removeDefaults.map {
      preFill => benefitForm.fill(preFill)
    }.getOrElse(benefitForm)

    replace_car_benefit_form(activeCarBenefit, primaryEmployment, filledForm, addDefaults, currentTaxYearYearsRange)(user)
  }

  def requestReplaceCarBenefit(taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(PayeRegime).async {
    implicit user =>
      implicit request =>
        implicit val hc = HeaderCarrier(request)
        validateVersionNumber(user, request.session).flatMap {
          _.fold(
            errorResult => Future.successful(errorResult),
            versionNumber => requestReplaceCarAction(taxYear, employmentSequenceNumber))
        }
  }

  private[paye] def showReplaceCarBenefitFormAction(user: User, request: Request[_], taxYear: Int, employmentSequenceNumber: Int): Future[SimpleResult] = {
    implicit val hc = HeaderCarrier(request)

    val f1 = user.getPaye.fetchTaxYearData(TaxYearResolver.currentTaxYear)
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
        Ok(replaceCarBenefit(activeCarBenefit, primaryEmployment, getDatesFromDefaults(removeFormData), removeFormData, addFormData, user))
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
      val removeForm = updateRemoveCarBenefitForm(rawData, activeCarBenefit.startDate, activeCarBenefit.hasActiveFuel, getCarFuelBenefitDates(request), now(), taxYearInterval).bindFromRequest()

      val dates = getCarBenefitDates(request)
      val addForm = carBenefitForm(dates).bindFromRequest()

      if (addForm.hasErrors || removeForm.hasErrors) {
        Future.successful(BadRequest(replace_car_benefit_form(activeCarBenefit, primaryEmployment, removeForm, addForm, currentTaxYearYearsRange)))
      } else {
        val addCarBenefitData = addForm.get
        val confirmationData = AddCarBenefitConfirmationData(Strings.optionalValue(primaryEmployment.employerName, "your.employer"), addCarBenefitData.providedFrom.getOrElse(startOfCurrentTaxYear),
          addCarBenefitData.listPrice.get, addCarBenefitData.fuelType.get, addCarBenefitData.co2Figure, addCarBenefitData.engineCapacity,
          addCarBenefitData.employerPayFuel, addCarBenefitData.dateFuelWithdrawn, addCarBenefitData.employeeContribution, addCarBenefitData.carRegistrationDate)
        keyStoreService.storeFormData(ReplaceCarBenefitFormData(removeForm.get, addCarBenefitData)).map { _ =>
          Ok(replace_car_benefit_review(activeCarBenefit, primaryEmployment, removeForm.get, confirmationData))
        }
      }
    }

    result.getOrElse(Future.successful(InternalServerError("")))
  }

  def confirmCarBenefitReplacementAction(taxYear: Int, employmentSequenceNumber: Int, version: Int)(implicit user: User, request: Request[_]) = {
    val taxYeadDataF = user.getPaye.fetchTaxYearData(TaxYearResolver.currentTaxYear)
    val taxCodeF = TaxCodeResolver.currentTaxCode(user.regimes.paye.get, employmentSequenceNumber, taxYear)
    val formDataF = keyStoreService.loadFormData

    for {
      taxYearData <- taxYeadDataF
      formDataO <- formDataF
      currentTaxCode <- taxCodeF
      result <- doUpdate(formDataO, taxYearData, employmentSequenceNumber, version, taxYear, currentTaxCode)
    } yield result
  }

  private def doUpdate(formDataO: Option[ReplaceCarBenefitFormData], taxYearData: TaxYearData, employmentSequenceNumber: Int, version: Int, taxYear: Int, currentTaxCode: String)
                      (implicit user: User, hc: HeaderCarrier): Future[SimpleResult] = {
    val result = for {
      formData <- formDataO
      activeCarBenefit <- taxYearData.findActiveCarBenefit(employmentSequenceNumber)
    } yield {
      val url = activeCarBenefit.actions.getOrElse("replace",
        throw new IllegalArgumentException(s"No remove action uri found for this car benefit."))

      payeConnector.replaceBenefits(url, buildRequest(version, formData, taxYear, employmentSequenceNumber)).map {
        case Some(response) => {
          keyStoreService.clearFormData
          Ok(replace_benefit_confirmation(currentTaxCode, response.taxCode)(user))
        }
      }
    }
    result.getOrElse(Future.successful(InternalServerError("Either form data was missing or there was no active car benefit")))
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
      employerContributes = defaults.employerContributes.map(_.toString),
      fuelType = defaults.fuelType,
      co2Figure = defaults.co2Figure.map(_.toString),
      co2NoFigure = defaults.co2NoFigure.map(_.toString),
      employerPayFuel = defaults.employerPayFuel)


  implicit class ReplaceBenefitKeyStore(keyStoreService: KeyStoreConnector) {
    val actionId = "ReplaceCarBenefitFormData"
    val keystoreKey = "replace_benefit"

    def storeFormData(formData: ReplaceCarBenefitFormData)(implicit hc: HeaderCarrier) = {
      keyStoreService.addKeyStoreEntry(actionId, KeystoreUtils.source, keystoreKey, formData)
    }

    def loadFormData(implicit hc: HeaderCarrier) = {
      keyStoreService.getEntry[ReplaceCarBenefitFormData](actionId, KeystoreUtils.source, keystoreKey)
    }

    def clearFormData(implicit hc: HeaderCarrier) = {
      keyStoreService.deleteKeyStore(actionId, KeystoreUtils.source)
    }
  }

}



