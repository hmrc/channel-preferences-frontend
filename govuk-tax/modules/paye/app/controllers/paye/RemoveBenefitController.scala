package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain.{RevisedBenefit, Benefit, PayeRegime}
import play.api.mvc.{Result, Request}
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
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import models.paye.RemoveBenefitFormData
import uk.gov.hmrc.utils.TaxYearResolver
import org.joda.time.format.DateTimeFormat
import views.formatting.Dates

class RemoveBenefitController extends BaseController with SessionTimeoutWrapper with Benefits {

  def benefitRemovalForm(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    implicit user => request => benefitRemovalFormAction(user, request, benefitTypes, taxYear, employmentSequenceNumber)
  })

  def requestBenefitRemoval(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    implicit user => request => requestBenefitRemovalAction(user, request, benefitTypes, taxYear, employmentSequenceNumber)
  })

  def confirmBenefitRemoval(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    implicit user => request => confirmBenefitRemovalAction(user, request, benefitTypes, taxYear, employmentSequenceNumber)
  })

  def benefitRemoved(benefitTypes: String, year: Int, employmentSequenceNumber: Int, oid: String, newTaxCode: Option[String], personalAllowance: Option[Int]) = WithSessionTimeoutValidation(AuthorisedForIdaAction(Some(PayeRegime)) {
    implicit user => request => benefitRemovedAction(user, request, benefitTypes, year, employmentSequenceNumber, oid, newTaxCode, personalAllowance)
  })

  private[paye] val benefitRemovalFormAction: (User, Request[_], String, Int, Int) => Result = WithValidatedRequest {
    (request, user, benefit) => {
      val benefitStartDate = findStartDate(benefit.benefit, user.regimes.paye.get.get.benefits(TaxYearResolver.currentTaxYear))
      val benefitType = benefit.benefit.benefitType
      val dates = getCarFuelBenefitDates(request)

      if (benefitType == CAR) {
        val carWithUnremovedFuel = hasUnremovedFuelBenefit(user, benefit.benefit.employmentSequenceNumber)
        Ok(remove_car_benefit_form(benefit, carWithUnremovedFuel , updateBenefitForm(benefitStartDate, carWithUnremovedFuel, dates), TaxYearResolver.currentTaxYearYearsRange)(user))
      } else {
        Ok(remove_benefit_form(benefit,hasUnremovedCarBenefit(user,benefit.benefit.employmentSequenceNumber), updateBenefitForm(benefitStartDate, false, dates), TaxYearResolver.currentTaxYearYearsRange)(user))
      }
    }
  }

  private def getSecondBenefit(user: User, mainBenefit: Benefit, removeCar:Boolean): Option[Benefit] = {

    mainBenefit.benefitType match {
      case CAR => findExistingBenefit(user, mainBenefit.employmentSequenceNumber, FUEL)
      case FUEL if removeCar => findExistingBenefit(user, mainBenefit.employmentSequenceNumber, CAR)
      case _ => None
    }
  }

  private[paye] val requestBenefitRemovalAction: (User, Request[_], String, Int, Int) => Result = WithValidatedRequest {
    (request, user, benefit) => {
      val benefitStartDate = findStartDate(benefit.benefit, user.regimes.paye.get.get.benefits(TaxYearResolver.currentTaxYear))
      val carWithUnremovedFuel = (CAR == benefit.benefit.benefitType) && hasUnremovedFuelBenefit(user, benefit.benefit.employmentSequenceNumber)
      updateBenefitForm(benefitStartDate, carWithUnremovedFuel, getCarFuelBenefitDates(request)).bindFromRequest()(request).fold(
        errors => {
          benefit.benefit.benefitType match {
            case CAR => BadRequest(remove_car_benefit_form(benefit, hasUnremovedFuelBenefit(user, benefit.benefit.employmentSequenceNumber), errors, TaxYearResolver.currentTaxYearYearsRange)(user))
            case FUEL => BadRequest(remove_benefit_form(benefit, hasUnremovedCarBenefit(user,benefit.benefit.employmentSequenceNumber), errors, TaxYearResolver.currentTaxYearYearsRange)(user))
            case _ => Redirect(routes.BenefitHomeController.listBenefits())
          }
        },
        removeBenefitData => {

          val mainBenefitType = benefit.benefit.benefitType

          val secondBenefit = getSecondBenefit(user, benefit.benefit, removeBenefitData.removeCar)

          val benefits = benefit.benefits ++ Seq(secondBenefit).filter(_.isDefined).map(_.get)

          val finalAndRevisedAmounts = benefits.foldLeft(BigDecimal(0), mutable.Map[String, BigDecimal]())((runningAmounts, benefit) => {
            val revisedAmount = benefit.benefitType match {
              case FUEL if differentDateForFuel(removeBenefitData.fuelDateChoice) => calculateRevisedAmount (benefit, removeBenefitData.fuelWithdrawDate.get)
              case _ =>  calculateRevisedAmount (benefit, removeBenefitData.withdrawDate)
            }
            (benefit, removeBenefitData.withdrawDate)
            runningAmounts._2.put(benefit.benefitType.toString, revisedAmount)

            (runningAmounts._1 + (benefit.grossAmount - revisedAmount), runningAmounts._2)
          })

          val apportionedValues = finalAndRevisedAmounts._2.toMap
          val secondWithdrawDate = if(removeBenefitData.fuelWithdrawDate.isDefined) removeBenefitData.fuelWithdrawDate else Some(removeBenefitData.withdrawDate)
          
          val benefitsInfo:Map[String, BenefitInfo]  = mapBenefitsInfo(benefit.benefits(0), Some(removeBenefitData.withdrawDate), apportionedValues) ++ 
                                                        secondBenefit.map( mapBenefitsInfo(_, secondWithdrawDate, apportionedValues)).getOrElse(Nil)

          keyStoreMicroService.addKeyStoreEntry(user.oid, "paye_ui", "remove_benefit", RemoveBenefitData(removeBenefitData.withdrawDate, apportionedValues))

          mainBenefitType match {
            case CAR | FUEL => {
              val updatedBenefit = benefit.copy(benefits = benefits , benefitsInfo = benefitsInfo)
              Ok(remove_benefit_confirm(finalAndRevisedAmounts._1, updatedBenefit)(user))
            }
            case _ => Redirect(routes.BenefitHomeController.listBenefits())
          }
        }
      )
    }
  }

  private def mapBenefitsInfo(benefit:Benefit, withdrawDate:Option[LocalDate], values:Map[String, BigDecimal]):Map[String, BenefitInfo] = {
    val benefitType = benefit.benefitType.toString
    Map(benefitType -> getBenefitInfo(benefit, withdrawDate, values.get(benefitType)))
  }

  private def getBenefitInfo(benefit:Benefit, withdrawDate: Option[LocalDate], apportionedValue: Option[BigDecimal])  = {
      val pathIncludingStartDate = benefit.calculations(payeMicroService.calculationWithdrawKey)
      BenefitInfo(getStartDate(pathIncludingStartDate).map(formatDate(_)), withdrawDate.map(formatDate(_)), apportionedValue)
  }

  private final val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  private final val dateRegex ="""(\d\d\d\d-\d\d-\d\d)""".r
  
  private def getStartDate(path:String): Option[LocalDate] = {
    dateRegex.findFirstIn(path) match {
      case Some(date)=> Some(dateFormat.parseLocalDate(date))
      case _ => None
    }
  }

  private[paye] val confirmBenefitRemovalAction: (User, Request[_], String, Int, Int) => Result = WithValidatedRequest {
    (request, user, displayBenefit) => {
      val payeRoot = user.regimes.paye.get.get
      if (carRemovalMissesFuelRemoval(user, displayBenefit)) {
        BadRequest
      } else {
        loadFormDataFor(user) match {
          case Some(formData) => {
            val uri = displayBenefit.benefit.actions.getOrElse("remove",
              throw new IllegalArgumentException(s"No remove action uri found for benefit type ${displayBenefit.allBenefitsToString}"))

            val revisedBenefits = displayBenefit.benefits.map(b => RevisedBenefit(b, formData.revisedAmounts.getOrElse(b.benefitType.toString,
              throw new IllegalArgumentException(s"Unknown revised amount for benefit ${b.benefitType}"))))

            val removeBenefitResponse = payeMicroService.removeBenefits(uri, payeRoot.nino, payeRoot.version, revisedBenefits, formData.withdrawDate).get
            Redirect(routes.RemoveBenefitController.benefitRemoved(displayBenefit.allBenefitsToString,
              displayBenefit.benefit.taxYear, displayBenefit.benefit.employmentSequenceNumber, removeBenefitResponse.transaction.oid,
              removeBenefitResponse.calculatedTaxCode, removeBenefitResponse.personalAllowance))
          }
          case _ => Redirect(routes.BenefitHomeController.listBenefits())
        }
      }
    }
  }

  private[paye] val benefitRemovedAction: (User, Request[_], String, Int, Int, String, Option[String], Option[Int]) =>
      play.api.mvc.Result = (user, request, kinds, year, employmentSequenceNumber, oid, newTaxCode, personalAllowance) =>
    if (txQueueMicroService.transaction(oid, user.regimes.paye.get.get).isEmpty) {
      NotFound
    } else {
      keyStoreMicroService.deleteKeyStore(user.oid, "paye_ui")
      val removedKinds = DisplayBenefit.fromStringAllBenefit(kinds)
      if (removedKinds.exists(kind => kind == FUEL || kind == CAR)) {
        val removalData = RemoveBenefitConfirmationData(TaxCodeResolver.currentTaxCode(user.regimes.paye.get.get, employmentSequenceNumber, year),
          newTaxCode, personalAllowance, Dates.formatDate(TaxYearResolver.startOfCurrentTaxYear), Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear))
        Ok(remove_benefit_confirmation(removedKinds, removalData)(user))
      } else {
        Redirect(routes.BenefitHomeController.listBenefits())
      }
    }


  private def carRemovalMissesFuelRemoval(user: User, displayBenefit: DisplayBenefit) = {
    displayBenefit.benefits.exists(_.benefitType == CAR) && !displayBenefit.benefits.exists(_.benefitType == FUEL) && hasUnremovedFuelBenefit(user, displayBenefit.benefit.employmentSequenceNumber)
  }

  private def updateBenefitForm(benefitStartDate:Option[LocalDate],
                                carBenefitWithUnremovedFuelBenefit:Boolean,
                                dates:Option[CarFuelBenefitDates]) = Form[RemoveBenefitFormData](
    mapping(
      "withdrawDate" -> localDateMapping(benefitStartDate),
      "agreement" -> checked("error.paye.remove.benefit.accept.agreement"),
      "removeCar" -> boolean,
      "fuelRadio" -> validateFuelDateChoice(carBenefitWithUnremovedFuelBenefit),
      "fuelWithdrawDate" -> validateFuelDate(dates, benefitStartDate)
    )(RemoveBenefitFormData.apply)(RemoveBenefitFormData.unapply)
  )

  private def datesForm() = Form[CarFuelBenefitDates](
    mapping(
      "withdrawDate" -> dateTuple(false),
      "fuelRadio" -> optional(text)
    )(CarFuelBenefitDates.apply)(CarFuelBenefitDates.unapply)
  )

  private def getCarFuelBenefitDates(request:Request[_]):Option[CarFuelBenefitDates] = {
    datesForm.bindFromRequest()(request).value
  }

  private def findStartDate(thisBenefit: Benefit, allBenefits: Seq[Benefit]): Option[LocalDate] = {
    thisBenefit.benefitType match {
      case CAR => thisBenefit.car.get.dateCarMadeAvailable
      case FUEL =>  { val carBenefit = Benefit.findByTypeAndEmploymentNumber(allBenefits, thisBenefit.employmentSequenceNumber, CAR)
        carBenefit.flatMap(_.car.get.dateCarMadeAvailable) }
      case _ => None
    }
  }

  private def calculateRevisedAmount(benefit: Benefit, withdrawDate: LocalDate): BigDecimal = {
    val calculationResult = payeMicroService.calculateWithdrawBenefit(benefit, withdrawDate)
    calculationResult.result(benefit.taxYear.toString)
  }

  private def hasUnremovedFuelBenefit(user: User, employmentNumber: Int): Boolean = {
    findExistingBenefit(user, employmentNumber, FUEL).isDefined
  }

  private def hasUnremovedCarBenefit(user: User, employmentNumber: Int): Boolean = {
    findExistingBenefit(user, employmentNumber, CAR).isDefined
  }

  private def loadFormDataFor(user: User) = {
    keyStoreMicroService.getEntry[RemoveBenefitData](user.oid, "paye_ui", "remove_benefit")
  }

  object WithValidatedRequest {
    def apply(action: (Request[_], User, DisplayBenefit) => Result): (User, Request[_], String, Int, Int) => Result = {
      (user, request, benefitTypes, taxYear, employmentSequenceNumber) => {
        val emptyBenefit = DisplayBenefit(null, Seq.empty, None, None)
        val validBenefits = DisplayBenefit.fromStringAllBenefit(benefitTypes).map {
          kind => getBenefit(user, kind, taxYear, employmentSequenceNumber)
        }

        if (!validBenefits.contains(None) ) {
          val validMergedBenefit = validBenefits.map(_.get).foldLeft(emptyBenefit)((a: DisplayBenefit, b: DisplayBenefit) => mergeDisplayBenefits(a, b))
          action(request, user, validMergedBenefit)
        } else {
          redirectToBenefitHome(request, user)
        }
      }
    }

    private def getBenefit(user: User, kind: Int, taxYear: Int, employmentSequenceNumber: Int): Option[DisplayBenefit] = {

      kind match {
        case CAR | FUEL => {
          if (thereAreNoExistingTransactionsMatching(user, kind, employmentSequenceNumber, taxYear)) {
            getBenefitMatching(kind, user, employmentSequenceNumber) match {
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

    private def getBenefitMatching(kind: Int, user: User, employmentSequenceNumber: Int): Option[DisplayBenefit] = {
      val taxYear = TaxYearResolver.currentTaxYear
      val benefit = user.regimes.paye.get.get.benefits(taxYear).find(
        b => b.employmentSequenceNumber == employmentSequenceNumber && b.benefitType == kind)

      val transactions = user.regimes.paye.get.get.recentCompletedTransactions

      val matchedBenefits = DisplayBenefits(benefit.toList, user.regimes.paye.get.get.employments(taxYear), transactions)

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


    private val redirectToBenefitHome: (Request[_], User) => Result = (r, u) => Redirect(routes.BenefitHomeController.listBenefits())
  }

}

case class RemoveBenefitData(withdrawDate: LocalDate, revisedAmounts: Map[String, BigDecimal])
