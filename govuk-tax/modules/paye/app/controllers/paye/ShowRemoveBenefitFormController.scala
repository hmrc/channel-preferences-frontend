package controllers.paye

import scala.concurrent._
import org.joda.time.LocalDate
import play.api.mvc._

import config.DateTimeProvider
import controllers.common.{SessionTimeoutWrapper, BaseController}
import controllers.common.actions.{HeaderCarrier, Actions}
import controllers.paye.validation.RemoveBenefitFlow
import controllers.common.service.Connectors

import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.paye.domain._
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector

import models.paye.{RemoveBenefitFormData, DisplayBenefit, CarFuelBenefitDates}
import views.html.paye.{remove_benefit_form, remove_car_benefit_form}
import play.api.data.Form

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

  def showBenefitRemovalForm(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(PayeRegime).async {
    user => request => showRemovalFormAction(user, request, benefitTypes, taxYear, employmentSequenceNumber)
  }

  private[paye] val showRemovalFormAction: (User, Request[_], String, Int, Int) => Future[SimpleResult] = RemoveBenefitFlow {
    (user: User, request: Request[_], benefit: DisplayBenefit, taxYearData: TaxYearData) =>
      implicit val hc = HeaderCarrier(request)

      keyStoreService.loadBenefitFormData.map { defaults =>
        val benefitStartDate = getStartDate(benefit.benefit)
        val dates = defaults.map { formData =>
          CarFuelBenefitDates(Some(formData.withdrawDate), formData.fuelDateChoice)
        }.orElse {
          Some(CarFuelBenefitDates(None, None))
        }

        benefit.benefit.benefitType match {
          case BenefitTypes.CAR => Ok(removeCarBenefit(benefit, taxYearData, benefitStartDate, dates, defaults, user))
          case _ => Ok(removeBenefit(benefit, taxYearData, benefitStartDate, dates, defaults, user))
        }
      }
  }

  def removeCarBenefit(benefit: DisplayBenefit, payeRootData: TaxYearData, benefitStartDate: LocalDate, dates: Option[CarFuelBenefitDates], defaults: Option[RemoveBenefitFormData], user: User) = {
    val hasUnremovedFuel = hasUnremovedFuelBenefit(payeRootData, benefit.benefit.employmentSequenceNumber)

    val benefitForm: Form[RemoveBenefitFormData] = updateBenefitForm(benefitStartDate, hasUnremovedFuel, dates, now(), taxYearInterval)
    val filledForm = defaults.map { preFill => benefitForm.fill(preFill)}.getOrElse(benefitForm)

    remove_car_benefit_form(benefit, hasUnremovedFuel, filledForm, currentTaxYearYearsRange)(user)
  }

  def removeBenefit(benefit: DisplayBenefit, payeRootData: TaxYearData, benefitStartDate: LocalDate, dates: Option[CarFuelBenefitDates], defaults: Option[RemoveBenefitFormData], user: User) = {
    val hasUnremovedCar = hasUnremovedCarBenefit(payeRootData, benefit.benefit.employmentSequenceNumber)

    val benefitForm: Form[RemoveBenefitFormData] = updateBenefitForm(benefitStartDate, carBenefitWithUnremovedFuelBenefit = false, dates, now(), taxYearInterval)
    val filledForm = defaults.map { preFill => benefitForm.fill(preFill)}.getOrElse(benefitForm)

    remove_benefit_form(benefit, hasUnremovedCar, filledForm, currentTaxYearYearsRange)(user)
  }
}
