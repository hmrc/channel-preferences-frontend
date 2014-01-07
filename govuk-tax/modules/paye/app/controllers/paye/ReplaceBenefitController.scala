package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain._
import play.api.mvc._
import views.html.paye._
import org.joda.time.{DateTimeZone, LocalDate}
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
import play.api.data.Forms._
import models.paye.CarBenefitData
import scala.Some
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.domain.TaxYearData
import controllers.paye.validation.AddCarBenefitValidator.CarBenefitValues
import CarBenefitFormFields._
import uk.gov.hmrc.utils.TaxYearResolver
import controllers.paye.validation.RemoveBenefitValidator.RemoveCarBenefitFormDataValues

class ReplaceBenefitController(keyStoreService: KeyStoreConnector, override val authConnector: AuthConnector, override val auditConnector: AuditConnector)
                             (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector)
  extends BaseController
  with Actions
  with SessionTimeoutWrapper
  with TaxYearSupport
  with PayeRegimeRoots {

  import RemovalUtils._

  def this() = this(Connectors.keyStoreConnector, Connectors.authConnector, Connectors.auditConnector)(Connectors.payeConnector, Connectors.txQueueConnector)

  def showReplaceCarBenefitForm(taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(PayeRegime).async {
    user =>
      request =>
        validateVersionNumber(user, request.session).fold(
          errorResult => Future.successful(errorResult),
          versionNumber => showReplaceCarBenefitFormAction(user, request, taxYear, employmentSequenceNumber))
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
        validateVersionNumber(user, request.session).fold(
          errorResult => Future.successful(errorResult),
          versionNumber => requestReplaceCarAction(taxYear, employmentSequenceNumber))
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
  def timeSource() = new LocalDate(DateTimeZone.UTC)


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

  private def getCarBenefitDates(request: Request[_]): CarBenefitValues = {
    validationlessForm.bindFromRequest()(request).value.get
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
        Ok(replace_car_benefit_review(activeCarBenefit, primaryEmployment, removeForm.get, addForm.get))
      }
    }

    Future.successful(result.getOrElse(InternalServerError("")))
  }

  private[paye] def validationlessForm = Form[CarBenefitValues](
    mapping(
      providedFrom -> dateTuple(false),
      carUnavailable -> optional(text),
      numberOfDaysUnavailable -> optional(text),
      giveBackThisTaxYear -> optional(text),
      providedTo -> dateTuple(false),
      carRegistrationDate -> dateTuple(false),
      employeeContributes -> optional(text),
      employerContributes -> optional(text),
      fuelType -> optional(text),
      co2Figure -> optional(text),
      co2NoFigure -> optional(text),
      employerPayFuel -> optional(text)
    )(CarBenefitValues.apply)(CarBenefitValues.unapply)
  )

  private def carBenefitForm(carBenefitValues: CarBenefitValues) = Form[CarBenefitData](
    mapping(
      providedFrom -> validateProvidedFrom(timeSource, taxYearInterval),
      carRegistrationDate -> validateCarRegistrationDate(timeSource),
      listPrice -> validateListPrice,
      employeeContributes -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
      employeeContribution -> validateEmployeeContribution(carBenefitValues),
      employerContributes -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
      employerContribution -> validateEmployerContribution(carBenefitValues),
      fuelType -> validateFuelType(carBenefitValues),
      co2Figure -> validateCo2Figure(carBenefitValues),
      co2NoFigure -> validateNoCo2Figure(carBenefitValues),
      engineCapacity -> validateEngineCapacity(carBenefitValues),
      employerPayFuel -> validateEmployerPayFuel(carBenefitValues),
      dateFuelWithdrawn -> validateDateFuelWithdrawn(carBenefitValues, taxYearInterval)
    )(CarBenefitData.apply)(CarBenefitData.unapply)
  )
}


