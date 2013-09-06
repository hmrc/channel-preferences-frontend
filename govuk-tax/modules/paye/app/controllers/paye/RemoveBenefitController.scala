package controllers.paye

import uk.gov.hmrc.microservice.paye.domain.{ Benefit, PayeRegime }
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
          Ok(remove_car_benefit_form(benefit, hasFuelBenefit(user), updateBenefitForm))
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
              case 31 => BadRequest(remove_car_benefit_form(benefit, hasFuelBenefit(user), errors))
              case 29 => BadRequest(remove_benefit_form(benefit, errors))
              case _ => Redirect(routes.BenefitHomeController.listBenefits())
            }
          },
          removeBenefitData => {
            val selectedBenefit = benefit.benefit

            var revisedAmount = calculateRevisedAmount(selectedBenefit, removeBenefitData.withdrawDate)
            var benefitTitle = "company car"
            var grossAmount = selectedBenefit.grossAmount

            if (removeBenefitData.removeFuel) {
              val fuelBenefit = getFuelBenefit(user).get

              revisedAmount = calculateRevisedAmount(fuelBenefit, removeBenefitData.withdrawDate)
              benefitTitle += " and fuel"
              grossAmount = grossAmount + fuelBenefit.grossAmount
            }

            keyStoreMicroService.addKeyStoreEntry(user.oid, "paye_ui", "remove_benefit", Map("form" -> RemoveBenefitData(removeBenefitData.withdrawDate, revisedAmount.toString())))

            benefit.benefit.benefitType match {
              case 31 => Ok(remove_car_benefit_confirm(grossAmount - revisedAmount, selectedBenefit, benefitTitle))
              case 29 => Ok(remove_benefit_confirm(revisedAmount, selectedBenefit))
              case _ => Redirect(routes.BenefitHomeController.listBenefits())
            }
          }
        )
      }
  }

  private def calculateRevisedAmount(benefit:Benefit, withdrawDate:LocalDate):BigDecimal = {
    val calculationResult = payeMicroService.calculateWithdrawBenefit(benefit, withdrawDate)
    calculationResult.result(benefit.taxYear.toString)
  }

  private def getFuelBenefit(user: User): Option[Benefit] = {
    val benefits = user.regimes.paye.get.benefits(currentTaxYear)
    benefits.find(b => b.benefitType == 29)
  }
  private def hasFuelBenefit(user: User): Boolean = {
    getFuelBenefit(user).isDefined
  }

  private lazy val updateBenefitForm = Form[RemoveBenefitFormData](
    mapping(
      "withdrawDate" -> localDateMapping,
      "agreement" -> checked("error.paye.remove.carbenefit.accept.agreement"),
      "removeFuel" -> boolean
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

        val formData: Option[RemoveBenefitData] = loadFormDataFor(user)

        if (formData.isDefined) {
          val uri = benefit.benefit.actions.getOrElse("remove", throw new IllegalArgumentException(s"No remove action uri found for benefit type ${benefit.benefit.benefitType}"))
          val transactionId = payeMicroService.removeBenefit(uri, payeRoot.nino, payeRoot.version, benefit.benefit, formData.get.withdrawDate, BigDecimal(formData.get.revisedAmount))
          Redirect(routes.RemoveBenefitController.benefitRemoved(benefit.benefit.benefitType, transactionId.get.oid))
        } else {
          Redirect(routes.BenefitHomeController.listBenefits())
        }
      }
  }

  def benefitRemoved(kind: Int, oid: String) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    user => request => benefitRemovedAction(user, request, kind, oid)
  })

  private[paye] val benefitRemovedAction: (User, Request[_], Int, String) => play.api.mvc.Result = (user, request, kind, oid) =>
    if (txQueueMicroService.transaction(oid, user.regimes.paye.get).isEmpty) {
      NotFound
    } else {
      val formData: Option[RemoveBenefitData] = loadFormDataFor(user)

      if (formData.isDefined) {

        keyStoreMicroService.deleteKeyStore(user.oid, "paye_ui")

        kind match {
          case 31 => Ok(remove_car_benefit_confirmation(Dates.formatDate(formData.get.withdrawDate), oid))
          case 29 => Ok(remove_benefit_confirmation(Dates.formatDate(formData.get.withdrawDate), kind, oid))
          case _ => Redirect(routes.BenefitHomeController.listBenefits())
        }
      } else {
        Redirect(routes.BenefitHomeController.listBenefits())
      }
    }

  private def loadFormDataFor(user: User) = {
    keyStoreMicroService.getEntry[RemoveBenefitData](user.oid, "paye_ui", "remove_benefit", "form")
  }
}

case class RemoveBenefitData(withdrawDate: LocalDate, revisedAmount: String)