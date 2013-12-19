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
import controllers.paye.validation.{BenefitFlowHelper, RemoveBenefitFlow}
import scala.Some
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import models.paye.BenefitUpdatedConfirmationData
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


  def requestBenefitRemoval(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(PayeRegime).async {
    user => request => requestBenefitRemovalAction(user, request, benefitTypes, taxYear, employmentSequenceNumber)
  }

  def confirmBenefitRemoval(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(PayeRegime).async {
    user => request => confirmBenefitRemovalAction(user, request, benefitTypes, taxYear, employmentSequenceNumber)
  }

  def benefitRemoved(benefitTypes: String, year: Int, employmentSequenceNumber: Int, oid: String, newTaxCode: Option[String], personalAllowance: Option[Int]) =
    AuthorisedFor(PayeRegime).async {
      user =>
        implicit request =>
          benefitRemovedAction(user, request, benefitTypes, year, employmentSequenceNumber, oid, newTaxCode, personalAllowance).removeSessionKey(BenefitFlowHelper.npsVersionKey)
    }


  private def getSecondBenefit(payeRootData: TaxYearData, mainBenefit: Benefit): Option[Benefit] = {
    mainBenefit.benefitType match {
      case CAR => payeRootData.findActiveBenefit(mainBenefit.employmentSequenceNumber, FUEL)
      case _ => None
    }
  }

  private[paye] val requestBenefitRemovalAction: (User, Request[_], String, Int, Int) => Future[SimpleResult] = RemoveBenefitFlow {
    (user, request, benefitToUpdate, payeRootData) => {
      val benefitStartDate = getStartDate(benefitToUpdate.benefit)

      val secondBenefit = getSecondBenefit(payeRootData, benefitToUpdate.benefit)
      val benefit = benefitToUpdate.copy(benefits = benefitToUpdate.benefits ++ Seq(secondBenefit).filter(_.isDefined).map(_.get))

      benefit.benefit.benefitType match {

        case CAR =>
          val carWithUnremovedFuel = (CAR == benefit.benefit.benefitType) && hasUnremovedFuelBenefit(payeRootData, benefit.benefit.employmentSequenceNumber)
          val rawData = Some(validationlessForm.bindFromRequest()(request).value.get)
          updateRemoveCarBenefitForm(rawData, benefitStartDate, carWithUnremovedFuel, getCarFuelBenefitDates(request), now(), taxYearInterval).bindFromRequest()(request).fold(
            errors => {
              val result = BadRequest(remove_car_benefit_form(benefit, hasUnremovedFuelBenefit(payeRootData, benefit.benefit.employmentSequenceNumber), errors, currentTaxYearYearsRange)(user))
              Future.successful(result)
            },
            removeBenefitData => {
              implicit def hc = HeaderCarrier(request)
              keyStoreService.storeBenefitFormData(removeBenefitData).map {_=>
                Ok(remove_benefit_confirm(benefit, removeBenefitData)(user))
              }
            }
          )
        case FUEL =>
          updateRemoveFuelBenefitForm(benefitStartDate, now(), taxYearInterval).bindFromRequest()(request).fold(
            errors => {
              val result = BadRequest(remove_fuel_benefit_form(benefit, errors, currentTaxYearYearsRange)(user))
              Future.successful(result)
            },
            removeBenefitData => {
              implicit def hc = HeaderCarrier(request)
              keyStoreService.storeBenefitFormData(removeBenefitData).map {_=>
                Ok(remove_benefit_confirm(benefit, RemoveCarBenefitFormData(removeBenefitData))(user))
              }
            }

          )
        case _ =>
          Logger.error(s"Unsupported benefit type for validation: ${benefit.benefit.benefitType}, redirecting to the car benefit homepage")
          Future.successful(Redirect(routes.CarBenefitHomeController.carBenefitHome()))

      }
    }
  }

  private final val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  private final val dateRegex = """(\d\d\d\d-\d\d-\d\d)""".r

  private def getStartDate(benefit: Benefit): LocalDate = {
    val pathIncludingStartDate = benefit.calculations.get(PayeConnector.calculationWithdrawKey).getOrElse("")

    val benefitStartDate = dateRegex.findFirstIn(pathIncludingStartDate) map {
      dateFormat.parseLocalDate
    }

    benefitStartDate match {
      case Some(dateOfBenefitStart) if dateOfBenefitStart.isAfter(startOfCurrentTaxYear) => dateOfBenefitStart
      case _ => startOfCurrentTaxYear
    }
  }

  private[paye] val confirmBenefitRemovalAction: (User, Request[_], String, Int, Int) => Future[SimpleResult] = RemoveBenefitFlow {
    (user, request, displayBenefit, payeRootData) => {
      val payeRoot = user.regimes.paye.get
      implicit def hc = HeaderCarrier(request)

      if (carRemovalMissesFuelRemoval(payeRootData, displayBenefit)) {
        Future.successful(BadRequest)
      } else {
        keyStoreService.loadBenefitFormData flatMap {
          case Some(formData) => {

            val uri = displayBenefit.benefit.actions.getOrElse("remove",
              throw new IllegalArgumentException(s"No remove action uri found for benefit type ${displayBenefit.allBenefitsToString}"))

            val request = displayBenefit.benefit.benefitType match {
              case CAR => {
                val carWithUnremovedFuel = (CAR == displayBenefit.benefit.benefitType) && hasUnremovedFuelBenefit(payeRootData, displayBenefit.benefit.employmentSequenceNumber)
                WithdrawnBenefitRequest(payeRoot.version, Some(WithdrawnCarBenefit(formData.withdrawDate, formData.numberOfDaysUnavailable, formData.employeeContribution)), if(carWithUnremovedFuel) Some(WithdrawnFuelBenefit(formData.fuelWithdrawDate.getOrElse(formData.withdrawDate))) else None)
              }
              case _ => WithdrawnBenefitRequest(payeRoot.version, None, Some(WithdrawnFuelBenefit(formData.withdrawDate)))
            }

            payeConnector.removeBenefits(uri, request).map(_.get).map { removeBenefitResponse =>
              keyStoreService.clearBenefitFormData

              Redirect(routes.RemoveBenefitController.benefitRemoved(displayBenefit.allBenefitsToString,
                displayBenefit.benefit.taxYear, displayBenefit.benefit.employmentSequenceNumber, removeBenefitResponse.transaction.oid,
                removeBenefitResponse.calculatedTaxCode, removeBenefitResponse.personalAllowance))
            }
          }
          case _ => {
            Logger.error(s"Cannot find keystore entry for user ${user.oid}, redirecting to car benefit homepage")
            Future.successful(Redirect(routes.CarBenefitHomeController.carBenefitHome()))
          }
        }
      }
    }
  }


  private[paye] val benefitRemovedAction: (User, Request[_], String, Int, Int, String, Option[String], Option[Int]) =>
    Future[SimpleResult] = (user, request, kinds, year, employmentSequenceNumber, oid, newTaxCode, personalAllowance) => {
    implicit def hc = HeaderCarrier(request)

    txQueueConnector.transaction(oid, user.regimes.paye.get).flatMap {
      case None => Future.successful(NotFound)
      case Some(tx) => {
        keyStoreService.clearBenefitFormData
        val removedKinds = DisplayBenefit.fromStringAllBenefit(kinds)
        if (removedKinds.exists(kind => kind == FUEL || kind == CAR)) {
          TaxCodeResolver.currentTaxCode(user.regimes.paye.get, employmentSequenceNumber, year).map { taxCode =>
            val removalData = BenefitUpdatedConfirmationData(taxCode, newTaxCode, personalAllowance, startOfCurrentTaxYear, endOfCurrentTaxYear)
            Ok(remove_benefit_confirmation(removedKinds, removalData)(user))
          }
        } else {
          Logger.error(s"Unsupported type of removed benefits: $kinds, redirecting to benefit list")
          Future.successful(Redirect(routes.CarBenefitHomeController.carBenefitHome()))
        }
      }
    }
  }


  private def carRemovalMissesFuelRemoval(payeRootData: TaxYearData, displayBenefit: DisplayBenefit) = {
    displayBenefit.benefits.exists(_.benefitType == CAR) && !displayBenefit.benefits.exists(_.benefitType == FUEL) && hasUnremovedFuelBenefit(payeRootData, displayBenefit.benefit.employmentSequenceNumber)
  }

  private def getCarFuelBenefitDates(request: Request[_]): Option[CarFuelBenefitDates] = {
    datesForm().bindFromRequest()(request).value
  }

  private def hasUnremovedFuelBenefit(payeRootData: TaxYearData, employmentNumber: Int): Boolean = {
    payeRootData.findActiveBenefit(employmentNumber, FUEL).isDefined
  }
}


