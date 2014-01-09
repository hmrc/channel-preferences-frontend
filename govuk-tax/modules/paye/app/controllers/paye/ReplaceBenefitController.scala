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
          versionNumber => confirmCarBenefitReplacementAction(taxYear, employmentSequenceNumber))
        }
  }

  def replaceCarBenefit(activeCarBenefit: CarBenefit, primaryEmployment: Employment, dates: Option[CarFuelBenefitDates], defaults: Option[RemoveCarBenefitFormData], user: User) = {
    val hasUnremovedFuel = activeCarBenefit.hasActiveFuel
    val benefitValues: Option[RemoveCarBenefitFormDataValues] = defaults.map(RemoveCarBenefitFormDataValues(_))
    val benefitForm: Form[RemoveCarBenefitFormData] = updateRemoveCarBenefitForm(benefitValues, activeCarBenefit.startDate, hasUnremovedFuel, dates, now(), taxYearInterval)
    val filledForm = defaults.map { preFill => benefitForm.fill(preFill)}.getOrElse(benefitForm)

    replace_car_benefit_form(activeCarBenefit, primaryEmployment, filledForm, carBenefitForm(CarBenefitValues()), currentTaxYearYearsRange)(user)
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
    //    val f2 = keyStoreService.loadCarBenefitFormData

    for {
      taxYearData <- f1
    //      defaults <- f2
    } yield {
      for {
        activeCarBenefit <- taxYearData.findActiveCarBenefit(employmentSequenceNumber)
        primaryEmployment <- taxYearData.findPrimaryEmployment
      } yield {
        Ok(replaceCarBenefit(activeCarBenefit, primaryEmployment, Some(CarFuelBenefitDates(None, None)), None, user))
      }
    }.getOrElse(Redirect(routes.CarBenefitHomeController.carBenefitHome()))
  }

  private[paye] def requestReplaceCarAction(taxYear: Int, employmentSequenceNumber: Int)(implicit user: User, request: Request[_]): Future[SimpleResult] = {
    implicit val hc = HeaderCarrier(request)
    val f1 = user.getPaye.fetchTaxYearData(taxYear)
//    val f2 = keyStoreService.loadCarBenefitFormData

    for {
      taxYearData <- f1
//      defaults <- f2
      result <- validateRemoveCarBenefitForm(taxYearData, employmentSequenceNumber, None)
    } yield result
  }

  private def validateRemoveCarBenefitForm(taxYearData: TaxYearData, employmentSequenceNumber: Int, formData: Option[RemoveCarBenefitFormData])(implicit user: User, request: Request[_]): Future[SimpleResult] = {
    val result = for {
      activeCarBenefit <- taxYearData.findActiveCarBenefit(employmentSequenceNumber)
      primaryEmployment <- taxYearData.findPrimaryEmployment
    } yield {
      val rawData = Some(RemoveBenefitValidator.validationlessForm.bindFromRequest().value.get)
      val removeForm = updateRemoveCarBenefitForm(rawData, activeCarBenefit.startDate, activeCarBenefit.hasActiveFuel, getCarFuelBenefitDates(request), now(), taxYearInterval).bindFromRequest()

      val dates = getCarBenefitDates(request)
      val addForm = carBenefitForm(dates).bindFromRequest()

      if (addForm.hasErrors || removeForm.hasErrors) {
        BadRequest(replace_car_benefit_form(activeCarBenefit, primaryEmployment, removeForm, addForm, currentTaxYearYearsRange))
      } else {
        val addCarBenefitData = addForm.get
        val confirmationData = AddCarBenefitConfirmationData(Strings.optionalValue(primaryEmployment.employerName, "your.employer"), addCarBenefitData.providedFrom.getOrElse(startOfCurrentTaxYear),
          addCarBenefitData.listPrice.get, addCarBenefitData.fuelType.get, addCarBenefitData.co2Figure, addCarBenefitData.engineCapacity,
          addCarBenefitData.employerPayFuel, addCarBenefitData.dateFuelWithdrawn, addCarBenefitData.employeeContribution,  addCarBenefitData.carRegistrationDate)

        Ok(replace_car_benefit_review(activeCarBenefit, primaryEmployment, removeForm.get, confirmationData))
      }
    }

    Future.successful(result.getOrElse(InternalServerError("")))
  }

  def confirmCarBenefitReplacementAction(taxYear: Int, employmentSequenceNumber: Int)(implicit user: User, request: Request[_]) = {
    ???
  }
}


