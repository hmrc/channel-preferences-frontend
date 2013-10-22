package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain._
import play.api.mvc.{SimpleResult, Request}
import views.html.paye._
import views.formatting.Dates._
import play.api.data.Form
import play.api.data.Forms._
import org.joda.time.LocalDate
import models.paye._
import models.paye.BenefitTypes._
import controllers.common.{SessionTimeoutWrapper, BaseController}
import scala.collection.mutable
import controllers.paye.validation.RemoveBenefitValidator._
import org.joda.time.format.DateTimeFormat
import views.formatting.Dates
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import uk.gov.hmrc.common.microservice.paye.PayeMicroService
import controllers.common.service.MicroServices
import play.api.Logger
import models.paye.RemoveBenefitConfirmationData
import models.paye.BenefitInfo
import uk.gov.hmrc.common.microservice.domain.User
import models.paye.CarFuelBenefitDates
import uk.gov.hmrc.common.microservice.paye.domain.RevisedBenefit
import models.paye.RemoveBenefitFormData
import config.DateTimeProvider

class RemoveBenefitController(keyStoreService: KeyStoreMicroService, payeService: PayeMicroService)
  extends BaseController
  with SessionTimeoutWrapper
  with Benefits
  with TaxYearSupport
  with DateTimeProvider {

  def this() = this(MicroServices.keyStoreMicroService, MicroServices.payeMicroService)

  def benefitRemovalForm(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) = AuthorisedForIdaAction(Some(PayeRegime)) {
    user => request => benefitRemovalFormAction(user, request, benefitTypes, taxYear, employmentSequenceNumber)
  }

  def requestBenefitRemoval(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) = AuthorisedForIdaAction(Some(PayeRegime)) {
    user => request => requestBenefitRemovalAction(user, request, benefitTypes, taxYear, employmentSequenceNumber)
  }

  def confirmBenefitRemoval(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) = AuthorisedForIdaAction(Some(PayeRegime)) {
    user => request => confirmBenefitRemovalAction(user, request, benefitTypes, taxYear, employmentSequenceNumber)
  }

  def benefitRemoved(benefitTypes: String, year: Int, employmentSequenceNumber: Int, oid: String, newTaxCode: Option[String], personalAllowance: Option[Int]) =
    AuthorisedForIdaAction(Some(PayeRegime)) {
      user => request => benefitRemovedAction(user, request, benefitTypes, year, employmentSequenceNumber, oid, newTaxCode, personalAllowance)
    }

  private[paye] val benefitRemovalFormAction: (User, Request[_], String, Int, Int) => SimpleResult = WithValidatedRequest {
    (request, user, benefit, payeRootData) => {
      val benefitStartDate = getStartDate(benefit.benefit)
      val dates = getCarFuelBenefitDates(request)

      benefit.benefit.benefitType match {
        case CAR => {
          val carWithUnremovedFuel = hasUnremovedFuelBenefit(payeRootData, benefit.benefit.employmentSequenceNumber)
          Ok(remove_car_benefit_form(benefit, carWithUnremovedFuel, updateBenefitForm(benefitStartDate, carWithUnremovedFuel, dates), currentTaxYearYearRange)(user))
        }
        case _ => Ok(remove_benefit_form(benefit, hasUnremovedCarBenefit(payeRootData, benefit.benefit.employmentSequenceNumber), updateBenefitForm(benefitStartDate, false, dates), currentTaxYearYearRange)(user))
      }
    }
  }

  private def getSecondBenefit(payeRootData: PayeRootData, mainBenefit: Benefit, removeCar: Boolean): Option[Benefit] = {

    mainBenefit.benefitType match {
      case CAR => findExistingBenefit(mainBenefit.employmentSequenceNumber, FUEL, payeRootData)
      case FUEL if removeCar => findExistingBenefit(mainBenefit.employmentSequenceNumber, CAR, payeRootData)
      case _ => None
    }
  }

  private[paye] val requestBenefitRemovalAction: (User, Request[_], String, Int, Int) => SimpleResult = WithValidatedRequest {
    (request, user, benefit, payeRootData) => {
      val benefitStartDate = getStartDate(benefit.benefit)
      val carWithUnremovedFuel = (CAR == benefit.benefit.benefitType) && hasUnremovedFuelBenefit(payeRootData, benefit.benefit.employmentSequenceNumber)
      updateBenefitForm(benefitStartDate, carWithUnremovedFuel, getCarFuelBenefitDates(request)).bindFromRequest()(request).fold(
        errors => {
          benefit.benefit.benefitType match {
            case CAR => BadRequest(remove_car_benefit_form(benefit, hasUnremovedFuelBenefit(payeRootData, benefit.benefit.employmentSequenceNumber), errors, currentTaxYearYearRange)(user))
            case FUEL => BadRequest(remove_benefit_form(benefit, hasUnremovedCarBenefit(payeRootData, benefit.benefit.employmentSequenceNumber), errors, currentTaxYearYearRange)(user))
            case _ => Logger.error(s"Unsupported benefit type for validation: ${benefit.benefit.benefitType}, redirecting to benefit list"); Redirect(routes.BenefitHomeController.listBenefits())
          }
        },
        removeBenefitData => {

          val mainBenefitType = benefit.benefit.benefitType

          val secondBenefit = getSecondBenefit(payeRootData, benefit.benefit, removeBenefitData.removeCar)

          val benefits = benefit.benefits ++ Seq(secondBenefit).filter(_.isDefined).map(_.get)

          val finalAndRevisedAmounts = benefits.foldLeft(BigDecimal(0), mutable.Map[String, BigDecimal]())((runningAmounts, benefit) => {
            val revisedAmount = benefit.benefitType match {
              case FUEL if differentDateForFuel(removeBenefitData.fuelDateChoice) => calculateRevisedAmount(benefit, removeBenefitData.fuelWithdrawDate.get)
              case _ => calculateRevisedAmount(benefit, removeBenefitData.withdrawDate)
            }
            (benefit, removeBenefitData.withdrawDate)
            runningAmounts._2.put(benefit.benefitType.toString, revisedAmount)

            (runningAmounts._1 + (benefit.grossAmount - revisedAmount), runningAmounts._2)
          })

          val apportionedValues = finalAndRevisedAmounts._2.toMap
          val secondWithdrawDate = removeBenefitData.fuelWithdrawDate.getOrElse(removeBenefitData.withdrawDate)

          val benefitsInfo: Map[String, BenefitInfo] = mapBenefitsInfo(benefit.benefits(0), removeBenefitData.withdrawDate, apportionedValues) ++
            secondBenefit.map(mapBenefitsInfo(_, secondWithdrawDate, apportionedValues)).getOrElse(Nil)

          keyStoreService.addKeyStoreEntry(user.oid, "paye_ui", "remove_benefit", RemoveBenefitData(removeBenefitData.withdrawDate, apportionedValues))

          mainBenefitType match {
            case CAR | FUEL => {
              val updatedBenefit = benefit.copy(benefits = benefits, benefitsInfo = benefitsInfo)
              Ok(remove_benefit_confirm(finalAndRevisedAmounts._1, updatedBenefit)(user))
            }
            case _ => Logger.error(s"Unsupported type of the main benefit: ${mainBenefitType}, redirecting to benefit list"); Redirect(routes.BenefitHomeController.listBenefits())
          }
        }
      )
    }
  }

  private def mapBenefitsInfo(benefit: Benefit, withdrawDate: LocalDate, values: Map[String, BigDecimal]): Map[String, BenefitInfo] = {
    val benefitType = benefit.benefitType.toString
    Map(benefitType -> getBenefitInfo(benefit, withdrawDate, values(benefitType)))
  }

  private def getBenefitInfo(benefit: Benefit, withdrawDate: LocalDate, apportionedValue: BigDecimal) = {
    BenefitInfo(formatDate(getStartDate(benefit)), formatDate(withdrawDate), apportionedValue)
  }

  private final val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  private final val dateRegex = """(\d\d\d\d-\d\d-\d\d)""".r

  private def getStartDate(benefit: Benefit): LocalDate = {
    val pathIncludingStartDate = benefit.calculations.get(payeService.calculationWithdrawKey).getOrElse("")

    val benefitStartDate = dateRegex.findFirstIn(pathIncludingStartDate) map {dateFormat.parseLocalDate(_)}

    benefitStartDate match {
      case Some(date) if date.isAfter(startOfCurrentTaxYear) => date
      case _ => startOfCurrentTaxYear
    }
  }

  private[paye] val confirmBenefitRemovalAction: (User, Request[_], String, Int, Int) => SimpleResult = WithValidatedRequest {
    (request, user, displayBenefit, payeRootData) => {
      val payeRoot = user.regimes.paye.get
      if (carRemovalMissesFuelRemoval(payeRootData, displayBenefit)) {
        BadRequest
      } else {
        loadFormDataFor(user) match {
          case Some(formData) => {
            val uri = displayBenefit.benefit.actions.getOrElse("remove",
              throw new IllegalArgumentException(s"No remove action uri found for benefit type ${displayBenefit.allBenefitsToString}"))

            val revisedBenefits = displayBenefit.benefits.map(b => RevisedBenefit(b, formData.revisedAmounts.getOrElse(b.benefitType.toString,
              throw new IllegalArgumentException(s"Unknown revised amount for benefit ${b.benefitType}"))))

            val removeBenefitResponse = payeService.removeBenefits(uri, payeRoot.nino, payeRoot.version, revisedBenefits, formData.withdrawDate).get
            Redirect(routes.RemoveBenefitController.benefitRemoved(displayBenefit.allBenefitsToString,
              displayBenefit.benefit.taxYear, displayBenefit.benefit.employmentSequenceNumber, removeBenefitResponse.transaction.oid,
              removeBenefitResponse.calculatedTaxCode, removeBenefitResponse.personalAllowance))
          }
          case _ => Logger.error(s"Cannot find keystore entry for user ${user.oid}, redirecting to benefit list"); Redirect(routes.BenefitHomeController.listBenefits())
        }
      }
    }
  }

  private[paye] val benefitRemovedAction: (User, Request[_], String, Int, Int, String, Option[String], Option[Int]) =>
    play.api.mvc.SimpleResult = (user, request, kinds, year, employmentSequenceNumber, oid, newTaxCode, personalAllowance) =>
    if (txQueueMicroService.transaction(oid, user.regimes.paye.get).isEmpty) {
      NotFound
    } else {
      keyStoreService.deleteKeyStore(user.oid, "paye_ui")
      val removedKinds = DisplayBenefit.fromStringAllBenefit(kinds)
      if (removedKinds.exists(kind => kind == FUEL || kind == CAR)) {
        val removalData = RemoveBenefitConfirmationData(TaxCodeResolver.currentTaxCode(user.regimes.paye.get, employmentSequenceNumber, year),
          newTaxCode, personalAllowance, Dates.formatDate(startOfCurrentTaxYear), Dates.formatDate(endOfCurrentTaxYear))
        Ok(remove_benefit_confirmation(removedKinds, removalData)(user))
      } else {
        Logger.error(s"Unsupported type of removed benefits: ${kinds}, redirecting to benefit list")
        Redirect(routes.BenefitHomeController.listBenefits())
      }
    }


  private def carRemovalMissesFuelRemoval(payeRootData: PayeRootData, displayBenefit: DisplayBenefit) = {
    displayBenefit.benefits.exists(_.benefitType == CAR) && !displayBenefit.benefits.exists(_.benefitType == FUEL) && hasUnremovedFuelBenefit(payeRootData, displayBenefit.benefit.employmentSequenceNumber)
  }

  private def updateBenefitForm(benefitStartDate: LocalDate,
                                carBenefitWithUnremovedFuelBenefit: Boolean,
                                dates: Option[CarFuelBenefitDates]) = Form[RemoveBenefitFormData](
    mapping(
      "withdrawDate" -> localDateMapping(Some(benefitStartDate), now().toLocalDate()),
      "agreement" -> checked("error.paye.remove.benefit.accept.agreement"),
      "removeCar" -> boolean,
      "fuelRadio" -> validateFuelDateChoice(carBenefitWithUnremovedFuelBenefit),
      "fuelWithdrawDate" -> validateFuelDate(dates, Some(benefitStartDate))
    )(RemoveBenefitFormData.apply)(RemoveBenefitFormData.unapply)
  )

  private def datesForm() = Form[CarFuelBenefitDates](
    mapping(
      "withdrawDate" -> dateTuple(false),
      "fuelRadio" -> optional(text)
    )(CarFuelBenefitDates.apply)(CarFuelBenefitDates.unapply)
  )

  private def getCarFuelBenefitDates(request: Request[_]): Option[CarFuelBenefitDates] = {
    datesForm.bindFromRequest()(request).value
  }

  private def calculateRevisedAmount(benefit: Benefit, withdrawDate: LocalDate): BigDecimal = {
    val calculationResult = payeService.calculateWithdrawBenefit(benefit, withdrawDate)
    calculationResult.result(benefit.taxYear.toString)
  }

  private def hasUnremovedFuelBenefit(payeRootData: PayeRootData, employmentNumber: Int): Boolean = {
    findExistingBenefit(employmentNumber, FUEL, payeRootData).isDefined
  }

  private def hasUnremovedCarBenefit(payeRootData: PayeRootData, employmentNumber: Int): Boolean = {
    findExistingBenefit(employmentNumber, CAR, payeRootData).isDefined
  }

  private def loadFormDataFor(user: User) = {
    keyStoreService.getEntry[RemoveBenefitData](user.oid, "paye_ui", "remove_benefit")
  }

  object WithValidatedRequest {
    def apply(action: (Request[_], User, DisplayBenefit, PayeRootData) => SimpleResult): (User, Request[_], String, Int, Int) => SimpleResult = {
      (user, request, benefitTypes, taxYear, employmentSequenceNumber) => {
        val payeRootData = user.regimes.paye.get.fetchTaxYearData(currentTaxYear)

        val emptyBenefit = DisplayBenefit(null, Seq.empty, None, None)
        val validBenefits = DisplayBenefit.fromStringAllBenefit(benefitTypes).map {
          kind => getBenefit(kind, taxYear, employmentSequenceNumber, payeRootData)
        }

        if (!validBenefits.contains(None)) {
          val validMergedBenefit = validBenefits.map(_.get).foldLeft(emptyBenefit)((a: DisplayBenefit, b: DisplayBenefit) => mergeDisplayBenefits(a, b))
          action(request, user, validMergedBenefit, payeRootData)
        } else {
          Logger.error(s"The requested benefit is not a valid benefit (year: $taxYear, empl: $employmentSequenceNumber, types: $benefitTypes), redirecting to benefit list")
          redirectToBenefitHome(request, user)
        }
      }
    }

    private def getBenefit(kind: Int, taxYear: Int, employmentSequenceNumber: Int, payeRootData: PayeRootData): Option[DisplayBenefit] = {

      kind match {
        case CAR | FUEL => {
          if (thereAreNoExistingTransactionsMatching(kind, employmentSequenceNumber, taxYear, payeRootData)) {
            getBenefitMatching(kind, employmentSequenceNumber, payeRootData) match {
              case Some(benefit) => Some(benefit)
              case _ => None
            }
          } else {
            None
          }
        }
        case _ => None
      }
    }

    private def getBenefitMatching(kind: Int, employmentSequenceNumber: Int, payeRootData: PayeRootData): Option[DisplayBenefit] = {

      val benefit = payeRootData.taxYearBenefits.find(
        b => b.employmentSequenceNumber == employmentSequenceNumber && b.benefitType == kind)

      val matchedBenefits = DisplayBenefits(benefit.toList, payeRootData.taxYearEmployments, payeRootData.completedTransactions)

      if (matchedBenefits.size > 0) Some(matchedBenefits(0)) else None
    }

    private def mergeDisplayBenefits(db1: DisplayBenefit, db2: DisplayBenefit): DisplayBenefit = {

      def validOption[A](option1: Option[A], option2: Option[A]): Option[A] = {
        option1 match {
          case Some(value) => option1
          case None => option2
        }
      }

      db1.copy(
        benefits = db1.benefits ++ db2.benefits,
        car = validOption(db1.car, db2.car),
        transaction = validOption(db1.transaction, db2.transaction),
        employment = if (db1.employment != null) db1.employment else db2.employment
      )
    }


    private val redirectToBenefitHome: (Request[_], User) => SimpleResult = (r, u) => Redirect(routes.BenefitHomeController.listBenefits())
  }

}

case class RemoveBenefitData(withdrawDate: LocalDate, revisedAmounts: Map[String, BigDecimal])
