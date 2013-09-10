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
import models.paye.{ DisplayBenefit, RemoveBenefitFormData }
import models.paye.BenefitTypes._

class RemoveBenefitController extends PayeController with RemoveBenefitValidator {

  def benefitRemovalForm(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    user => request => benefitRemovalFormAction(user, request, benefitTypes, taxYear, employmentSequenceNumber)
  })

  def requestBenefitRemoval(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    user => request => requestBenefitRemovalAction(user, request, benefitTypes, taxYear, employmentSequenceNumber)
  })

  def confirmBenefitRemoval(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    user => request => confirmBenefitRemovalAction(user, request, benefitTypes, taxYear, employmentSequenceNumber)
  })

  def benefitRemoved(benefitTypes: String, oid: String) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    user => request => benefitRemovedAction(user, request, benefitTypes, oid)
  })

  private[paye] val benefitRemovalFormAction: (User, Request[_], String, Int, Int) => Result = WithValidatedRequest {
    (request, user, benefit) =>
      {
        if (benefit.benefit.benefitType == CAR) {
          Ok(remove_car_benefit_form(benefit, hasFuelBenefit(user), updateBenefitForm))
        } else {
          Ok(remove_benefit_form(benefit, updateBenefitForm))
        }
      }
  }

  private[paye] val requestBenefitRemovalAction: (User, Request[_], String, Int, Int) => Result = WithValidatedRequest {
    (request, user, benefit) =>
      {
        updateBenefitForm.bindFromRequest()(request).fold(
          errors => {
            benefit.benefit.benefitType match {
              case CAR => BadRequest(remove_car_benefit_form(benefit, hasFuelBenefit(user), errors))
              case FUEL => BadRequest(remove_benefit_form(benefit, errors))
              case _ => Redirect(routes.BenefitHomeController.listBenefits())
            }
          },
          removeBenefitData => {

            val fuelBenefit = if (removeBenefitData.removeFuel) getFuelBenefit(user) else None
            val updatedBenefit = benefit.copy(dependentBenefits = Seq(fuelBenefit).filter(_.isDefined).map(_.get))

            val finalAndRevisedAmounts = updatedBenefit.allBenefits.foldLeft((BigDecimal(0), BigDecimal(0)))((runningAmounts, benefit) => {
              val revisedAmount = calculateRevisedAmount(benefit, removeBenefitData.withdrawDate)
              (runningAmounts._1 + (benefit.grossAmount - revisedAmount), runningAmounts._2 + revisedAmount)
            })

            keyStoreMicroService.addKeyStoreEntry(user.oid, "paye_ui", "remove_benefit", Map("form" -> RemoveBenefitData(removeBenefitData.withdrawDate, finalAndRevisedAmounts._2.toString())))

            benefit.benefit.benefitType match {
              case CAR | FUEL => Ok(remove_benefit_confirm(finalAndRevisedAmounts._1, updatedBenefit))
              case _ => Redirect(routes.BenefitHomeController.listBenefits())
            }
          }
        )
      }
  }

  private def calculateRevisedAmount(benefit: Benefit, withdrawDate: LocalDate): BigDecimal = {
    val calculationResult = payeMicroService.calculateWithdrawBenefit(benefit, withdrawDate)
    calculationResult.result(benefit.taxYear.toString)
  }

  private def getFuelBenefit(user: User): Option[Benefit] = {
    val benefits = user.regimes.paye.get.benefits(currentTaxYear)
    benefits.find(b => b.benefitType == FUEL)
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

  private[paye] val confirmBenefitRemovalAction: (User, Request[_], String, Int, Int) => Result = WithValidatedRequest {
    (request, user, displayBenefit) =>
      {
        val payeRoot = user.regimes.paye.get

        loadFormDataFor(user) match {
          case Some(formData) => {
            val uri = displayBenefit.benefit.actions.getOrElse("remove", throw new IllegalArgumentException(s"No remove action uri found for benefit type ${displayBenefit.benefit.benefitType}"))
            val transactionId = payeMicroService.removeBenefits(uri, payeRoot.nino, payeRoot.version, Seq(displayBenefit.benefit) ++ displayBenefit.dependentBenefits, formData.withdrawDate, BigDecimal(formData.revisedAmount))
            Redirect(routes.RemoveBenefitController.benefitRemoved(displayBenefit.allBenefitsToString, transactionId.get.oid))
          }
          case _ => Redirect(routes.BenefitHomeController.listBenefits())
        }
      }
  }

  private[paye] val benefitRemovedAction: (User, Request[_], String, String) => play.api.mvc.Result = (user, request, kinds, oid) =>
    if (txQueueMicroService.transaction(oid, user.regimes.paye.get).isEmpty) {
      NotFound
    } else {
      loadFormDataFor(user) match {
        case Some(formData) => {
          keyStoreMicroService.deleteKeyStore(user.oid, "paye_ui")
          val removedKinds = DisplayBenefit.fromStringAllBenefit(kinds)
          if (removedKinds.exists(kind => kind == FUEL || kind == CAR)) {
            Ok(remove_benefit_confirmation(Dates.formatDate(formData.withdrawDate), removedKinds, oid))
          } else {
            Redirect(routes.BenefitHomeController.listBenefits())
          }
        }
        case None => Redirect(routes.BenefitHomeController.listBenefits())
      }
    }

  private def loadFormDataFor(user: User) = {
    keyStoreMicroService.getEntry[RemoveBenefitData](user.oid, "paye_ui", "remove_benefit", "form")
  }
}

case class RemoveBenefitData(withdrawDate: LocalDate, revisedAmount: String)