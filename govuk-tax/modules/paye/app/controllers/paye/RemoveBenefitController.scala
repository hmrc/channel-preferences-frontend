package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain.{RevisedBenefit, Benefit, PayeRegime}
import play.api.mvc.{Result, Request}
import views.html.paye._
import views.formatting.Dates
import play.api.data.Form
import play.api.data.Forms._
import org.joda.time.LocalDate
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import models.paye.{BenefitTypes, DisplayBenefits, DisplayBenefit, RemoveBenefitFormData}
import models.paye.BenefitTypes._
import uk.gov.hmrc.common.TaxYearResolver
import controllers.common.{SessionTimeoutWrapper, ActionWrappers, BaseController}
import scala.collection.mutable

class RemoveBenefitController extends BaseController with SessionTimeoutWrapper with Benefits {

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
    (request, user, benefit) => {
      val benefitStartDate = findStartDate(benefit.benefit, user.regimes.paye.get.benefits(TaxYearResolver()))
      val benefitType = benefit.benefit.benefitType
      val dates = datesForm.bindFromRequest()(request).get

      if (benefitType == CAR) {
        Ok(remove_car_benefit_form(benefit,
          hasUnremovedFuelBenefit(user, benefit.benefit.employmentSequenceNumber), updateBenefitForm(benefitStartDate, benefitType, dates)))
      } else {
        Ok(remove_benefit_form(benefit,hasUnremovedCarBenefit(user,benefit.benefit.employmentSequenceNumber), updateBenefitForm(benefitStartDate, benefitType, dates)))
      }
    }
  }

  case class RemoveBenefitFormDates(carDate: Option[LocalDate], fuelDate: Option[LocalDate])
  private def datesForm() = Form[RemoveBenefitFormDates](
    mapping(
      "withdrawDate" -> optional(jodaLocalDate),
      "fuel.withdrawDate" -> optional(jodaLocalDate)
    )(RemoveBenefitFormDates.apply)(RemoveBenefitFormDates.unapply)
  )

  private def dateIsBefore() = { //v:RemoveBenefitFormDates) = {
    //val c = v.carDate
    //val f = v.fuelDate
    //val (carDate, fuelDate) = datesForm.bindFromRequest.get
    val f = datesForm
    val x = f.get.carDate
    false
  }

  private def updateBenefitForm(benefitStartDate:Option[LocalDate],
                                benefitType:Int,
                                dates:RemoveBenefitFormDates) = Form[RemoveBenefitFormData](
    mapping(
      "withdrawDate" -> localDateMapping(benefitStartDate),
      "agreement" -> checked("error.paye.remove.carbenefit.accept.agreement"),
      "removeCar" -> boolean,
      "fuel" -> tuple(
        "radio" -> customFuelDateChoice(benefitType),
        "withdrawDate" -> optional(jodaLocalDate)
      ) .verifying( "Fuel date cannot be empty",  data => checkFuelDate(data._1, data._2))
        .verifying("date must be less than car date", data => dateIsLessThan(dates.carDate, dates.fuelDate))
    )(RemoveBenefitFormData.apply)(RemoveBenefitFormData.unapply)
  )

  private def dateIsLessThan(dateOne:Option[LocalDate], dateTwo:Option[LocalDate]) = {
    if (dateOne.isDefined && dateTwo.isDefined) {
      dateOne.get.compareTo(dateTwo.get) match {
        case 0 => false
        case -1 => false
        case _  => true
      }
    } else {
      true
    }
  }

  private def checkFuelDate (optionFuelDate:Option[String], date:Option[LocalDate]):Boolean = {
    optionFuelDate match {
      case Some(a) if a == "differentDateFuel" && date.isEmpty => false
      case _ => true
    }
  }

  private[paye] val requestBenefitRemovalAction: (User, Request[_], String, Int, Int) => Result = WithValidatedRequest {
    (request, user, benefit) => {
      val benefitStartDate = findStartDate(benefit.benefit, user.regimes.paye.get.benefits(TaxYearResolver()))
      updateBenefitForm(benefitStartDate, benefit.benefit.benefitType, datesForm.bindFromRequest()(request).get).bindFromRequest()(request).fold(
        errors => {
          benefit.benefit.benefitType match {
            case CAR => BadRequest(remove_car_benefit_form(benefit, hasUnremovedFuelBenefit(user, benefit.benefit.employmentSequenceNumber), errors))
            case FUEL => BadRequest(remove_benefit_form(benefit, hasUnremovedCarBenefit(user,benefit.benefit.employmentSequenceNumber), errors))
            case _ => Redirect(routes.BenefitHomeController.listBenefits())
          }
        },
        removeBenefitData => {
          val benefitType = benefit.benefit.benefitType
          val fuelBenefit = if (benefitType == CAR ) findExistingBenefit(user, benefit.benefit.employmentSequenceNumber, FUEL) else None
          val carBenefit = if (benefitType == FUEL && removeBenefitData.removeCar) findExistingBenefit(user, benefit.benefit.employmentSequenceNumber, CAR) else None

          val updatedBenefit = benefit.copy(benefits = benefit.benefits ++ Seq(fuelBenefit,carBenefit).filter(_.isDefined).map(_.get))

          val finalAndRevisedAmounts = updatedBenefit.benefits.foldLeft(BigDecimal(0), mutable.Map[String, BigDecimal]())((runningAmounts, benefit) => {
            val revisedAmount = benefit.benefitType match {
              case FUEL if removeBenefitData.fuelDateChoice._1.getOrElse("") == "differentDateFuel" => calculateRevisedAmount (benefit, removeBenefitData.fuelDateChoice._2.get)
              case _ =>  calculateRevisedAmount (benefit, removeBenefitData.withdrawDate)
            }
            (benefit, removeBenefitData.withdrawDate)
            runningAmounts._2.put(benefit.benefitType.toString, revisedAmount)

            (runningAmounts._1 + (benefit.grossAmount - revisedAmount), runningAmounts._2)
          })

          keyStoreMicroService.addKeyStoreEntry(user.oid, "paye_ui", "remove_benefit", RemoveBenefitData(removeBenefitData.withdrawDate, finalAndRevisedAmounts._2.toMap))

          benefitType match {
            case CAR | FUEL => Ok(remove_benefit_confirm(finalAndRevisedAmounts._1, updatedBenefit))
            case _ => Redirect(routes.BenefitHomeController.listBenefits())
          }
        }
      )
    }
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

  private def customFuelDateChoice(benefitType:Int) = optional(text)
    .verifying("error.paye.benefit.choice.mandatory", fuelDateChoice => isThereIfFuel(fuelDateChoice, benefitType))


  private def localDateMapping(benefitStartDate:Option[LocalDate]) = jodaLocalDate
    .verifying("error.paye.benefit.date.next.taxyear", date => date.isBefore(new LocalDate(TaxYearResolver() + 1, 4, 6)))
    .verifying("error.paye.benefit.date.greater.7.days", date => date.minusDays(7).isBefore(new LocalDate()))
    .verifying("error.paye.benefit.date.previous.taxyear", date => date.isAfter(new LocalDate(TaxYearResolver(), 4, 5)))
    .verifying("error.paye.benefit.date.previous.startdate", date => isAfter(date, benefitStartDate))

  private def isThereIfFuel(fuelDateChoice:Option[Any] , benefitType:Int):Boolean = {
    benefitType match {
      case CAR => fuelDateChoice.isDefined
      case _ => true
    }
  }

  private def isAfter(withdrawDate:LocalDate, startDate:Option[LocalDate]) : Boolean = {
   startDate match {
      case Some(startDate) => withdrawDate.isAfter(startDate)
      case None => true
    }
  }
  private[paye] val confirmBenefitRemovalAction: (User, Request[_], String, Int, Int) => Result = WithValidatedRequest {
    (request, user, displayBenefit) => {
      val payeRoot = user.regimes.paye.get
      if (carRemovalMissesFuelRemoval(user, displayBenefit)) {
        BadRequest
      } else {
        loadFormDataFor(user) match {
          case Some(formData) => {
            val uri = displayBenefit.benefit.actions.getOrElse("remove",
              throw new IllegalArgumentException(s"No remove action uri found for benefit type ${displayBenefit.allBenefitsToString}"))

            val revisedBenefits = displayBenefit.benefits.map(b => RevisedBenefit(b, formData.revisedAmounts.getOrElse(b.benefitType.toString,
              throw new IllegalArgumentException(s"Unknown revised amount for benefit ${b.benefitType}"))))

            val transactionId = payeMicroService.removeBenefits(uri, payeRoot.nino, payeRoot.version, revisedBenefits, formData.withdrawDate)
            Redirect(routes.RemoveBenefitController.benefitRemoved(displayBenefit.allBenefitsToString, transactionId.get.oid))
          }
          case _ => Redirect(routes.BenefitHomeController.listBenefits())
        }
      }
    }
  }

  private def carRemovalMissesFuelRemoval(user: User, displayBenefit: DisplayBenefit) = {
    displayBenefit.benefits.exists(_.benefitType == CAR) && !displayBenefit.benefits.exists(_.benefitType == FUEL) && hasUnremovedFuelBenefit(user, displayBenefit.benefit.employmentSequenceNumber)
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
      val taxYear = TaxYearResolver()
      val benefit = user.regimes.paye.get.benefits(taxYear).find(
        b => b.employmentSequenceNumber == employmentSequenceNumber && b.benefitType == kind)

      val transactions = user.regimes.paye.get.recentCompletedTransactions

      val matchedBenefits = DisplayBenefits(benefit.toList, user.regimes.paye.get.employments(taxYear), transactions)

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
