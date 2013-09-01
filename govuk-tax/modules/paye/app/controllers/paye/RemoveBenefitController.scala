package controllers.paye

import uk.gov.hmrc.microservice.paye.domain.PayeRegime
import play.api.mvc.{ Result, Request }
import views.html.paye._
import views.formatting.Dates
import play.api.data.Form
import play.api.data.Forms._
import org.joda.time.LocalDate
import scala.Some
import uk.gov.hmrc.microservice.domain.User
import models.paye.RemoveBenefitFormData

class RemoveBenefitController extends PayeController with RemoveBenefitValidator {

  def benefitRemovalForm(kind: Int, year: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    user => request => benefitRemovalFormAction(kind, user, request, year, employmentSequenceNumber)
  })

  private[paye] val benefitRemovalFormAction: (Int, User, Request[_], Int, Int) => Result = WithValidatedRequest {
    (request, user, benefit) =>
      {
        if (benefit.benefit.benefitType == 31) {
          Ok(remove_car_benefit_form(benefit, updateBenefitForm))
        } else {
          Ok(remove_benefit_form(benefit, updateBenefitForm))
        }
      }
  }

  def requestBenefitRemoval(kind: Int, year: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    user => request => requestBenefitRemovalAction(kind, user, request, year, employmentSequenceNumber)
  })

  private[paye] val requestBenefitRemovalAction: (Int, User, Request[_], Int, Int) => Result = WithValidatedRequest {
    (request, user, benefit) =>
      {
        updateBenefitForm.bindFromRequest()(request).fold(
          errors => {
            benefit.benefit.benefitType match {
              case 31 => BadRequest(remove_car_benefit_form(benefit, errors))
              case 29 => BadRequest(remove_benefit_form(benefit, errors))
              case _ => Redirect(routes.BenefitHomeController.listBenefits)
            }
          },
          removeBenefitData => {
            val calculationResult = payeMicroService.calculateWithdrawBenefit(benefit.benefit, removeBenefitData.withdrawDate)
            val revisedAmount = calculationResult.result(benefit.benefit.taxYear.toString)

            benefit.benefit.benefitType match {
              case 31 => Ok(remove_car_benefit_confirm(revisedAmount, benefit.benefit)).withSession(request.session
                + ("withdraw_date", Dates.shortDate(removeBenefitData.withdrawDate))
                + ("revised_amount", revisedAmount.toString()))
              case 29 => Ok(remove_benefit_confirm(revisedAmount, benefit.benefit)).withSession(request.session
                + ("withdraw_date", Dates.shortDate(removeBenefitData.withdrawDate))
                + ("revised_amount", revisedAmount.toString()))
              case _ => Redirect(routes.BenefitHomeController.listBenefits)
            }
          }
        )
      }
  }

  private lazy val updateBenefitForm = Form[RemoveBenefitFormData](
    mapping(
      "withdrawDate" -> localDateMapping,
      "agreement" -> checked("error.paye.remove.carbenefit.accept.agreement")
    )(RemoveBenefitFormData.apply)(RemoveBenefitFormData.unapply)
  )

  private lazy val localDateMapping = jodaLocalDate
    .verifying("error.paye.benefit.date.next.taxyear", date => date.isBefore(new LocalDate(currentTaxYear + 1, 4, 6)))
    .verifying("error.paye.benefit.date.greater.7.days", date => date.minusDays(7).isBefore(new LocalDate()))
    .verifying("error.paye.benefit.date.previous.taxyear", date => date.isAfter(new LocalDate(currentTaxYear, 4, 5)))

  def confirmBenefitRemoval(kind: Int, year: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    user => request => confirmBenefitRemovalAction(kind, user, request, year, employmentSequenceNumber)
  })

  private[paye] val confirmBenefitRemovalAction: (Int, User, Request[_], Int, Int) => Result = WithValidatedRequest {
    (request, user, benefit) =>
      {
        val payeRoot = user.regimes.paye.get
        val withdrawDate = request.session.get("withdraw_date").get
        val revisedAmount = request.session.get("revised_amount").get
        val uri = benefit.benefit.benefitType match {
          case 31 => benefit.benefit.actions("removeCar")
          case 29 => benefit.benefit.actions("removeFuel")
          case _ => throw new IllegalArgumentException(s"No action uri found for benefit type ${benefit.benefit.benefitType}")
        }
        val transactionId = payeMicroService.removeBenefit(uri, payeRoot.nino, payeRoot.version, benefit.benefit, Dates.parseShortDate(withdrawDate), BigDecimal(revisedAmount))

        Redirect(routes.RemoveBenefitController.benefitRemoved(benefit.benefit.benefitType, transactionId.get.oid))
      }
  }

  def benefitRemoved(kind: Int, oid: String) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    user => request => benefitRemovedAction(user, request, kind, oid)
  })

  private[paye] val benefitRemovedAction: (User, Request[_], Int, String) => play.api.mvc.Result = (user, request, kind, oid) =>
    if (txQueueMicroService.transaction(oid, user.regimes.paye.get).isEmpty) {
      NotFound
    } else {
      kind match {
        case 31 => Ok(remove_car_benefit_confirmation(Dates.formatDate(Dates.parseShortDate(request.session.get("withdraw_date").get)), oid))
        case 29 => Ok(remove_benefit_confirmation(Dates.formatDate(Dates.parseShortDate(request.session.get("withdraw_date").get)), kind, oid))
        case _ => Redirect(routes.BenefitHomeController.listBenefits)
      }
    }
}
