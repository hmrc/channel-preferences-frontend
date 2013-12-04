package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain._
import play.api.mvc._
import views.html.paye._
import views.formatting.Dates._
import org.joda.time.LocalDate
import models.paye._
import controllers.common.{BaseController, SessionTimeoutWrapper}
import controllers.paye.validation.RemoveBenefitValidator._
import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import controllers.common.service.Connectors
import play.api.Logger
import config.DateTimeProvider
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import models.paye.BenefitUpdatedConfirmationData
import models.paye.BenefitInfo
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.paye.domain.TaxYearData
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.domain.RevisedBenefit
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import controllers.common.actions.{HeaderCarrier, Actions}
import BenefitTypes._
import scala.concurrent._
import play.api.data._
import play.api.data.Forms._
import controllers.paye.validation.{BenefitFlowHelper, RemoveBenefitFlow}

class RemoveBenefitController(keyStoreService: KeyStoreConnector, override val authConnector: AuthConnector, override val auditConnector: AuditConnector)(implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector) extends BaseController
with Actions
with SessionTimeoutWrapper
with TaxYearSupport
with DateTimeProvider
with PayeRegimeRoots {

  def this() = this(Connectors.keyStoreConnector, Connectors.authConnector, Connectors.auditConnector)(Connectors.payeConnector, Connectors.txQueueConnector)

  val keystoreKey = "remove_benefit"

  def benefitRemovalForm(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(PayeRegime).async {
    user => request => benefitRemovalFormAction(user, request, benefitTypes, taxYear, employmentSequenceNumber)
  }

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

  private[paye] val benefitRemovalFormAction: (User, Request[_], String, Int, Int) => Future[SimpleResult] = RemoveBenefitFlow {
    (user, request, benefit, payeRootData) =>
      future {
        val benefitStartDate = getStartDate(benefit.benefit)
        val dates = getCarFuelBenefitDates(request)

        benefit.benefit.benefitType match {
          case CAR => {
            val carWithUnremovedFuel = hasUnremovedFuelBenefit(payeRootData, benefit.benefit.employmentSequenceNumber)
            Ok(remove_car_benefit_form(benefit, carWithUnremovedFuel, updateBenefitForm(benefitStartDate, carWithUnremovedFuel, dates), currentTaxYearYearsRange)(user))
          }
          case _ => Ok(remove_benefit_form(benefit, hasUnremovedCarBenefit(payeRootData, benefit.benefit.employmentSequenceNumber), updateBenefitForm(benefitStartDate, false, dates), currentTaxYearYearsRange)(user))
        }
      }
  }

  private def getSecondBenefit(payeRootData: TaxYearData, mainBenefit: Benefit, removeCar: Boolean): Option[Benefit] = {

    mainBenefit.benefitType match {
      case CAR => payeRootData.findExistingBenefit(mainBenefit.employmentSequenceNumber, FUEL)
      case FUEL if removeCar => payeRootData.findExistingBenefit(mainBenefit.employmentSequenceNumber, CAR)
      case _ => None
    }
  }

  private[paye] val requestBenefitRemovalAction: (User, Request[_], String, Int, Int) => Future[SimpleResult] = RemoveBenefitFlow {
    (user, request, benefit, payeRootData) =>
      future {
        val benefitStartDate = getStartDate(benefit.benefit)
        val carWithUnremovedFuel = (CAR == benefit.benefit.benefitType) && hasUnremovedFuelBenefit(payeRootData, benefit.benefit.employmentSequenceNumber)
        implicit def hc = HeaderCarrier(request)

        updateBenefitForm(benefitStartDate, carWithUnremovedFuel, getCarFuelBenefitDates(request)).bindFromRequest()(request).fold(
          errors => {
            benefit.benefit.benefitType match {
              case CAR => BadRequest(remove_car_benefit_form(benefit, hasUnremovedFuelBenefit(payeRootData, benefit.benefit.employmentSequenceNumber), errors, currentTaxYearYearsRange)(user))
              case FUEL => BadRequest(remove_benefit_form(benefit, hasUnremovedCarBenefit(payeRootData, benefit.benefit.employmentSequenceNumber), errors, currentTaxYearYearsRange)(user))
              case _ => Logger.error(s"Unsupported benefit type for validation: ${benefit.benefit.benefitType}, redirecting to the car benefit homepage"); Redirect(routes.CarBenefitHomeController.carBenefitHome())
            }
          },
          removeBenefitData => removeBenefit(user, benefit, payeRootData, removeBenefitData)
        )
      }
  }

  def removeBenefit(user: User, benefit: DisplayBenefit, payeRootData: TaxYearData, removeBenefitData: RemoveBenefitFormData)(implicit hc: HeaderCarrier): SimpleResult = {
    val mainBenefitType = benefit.benefit.benefitType
    val secondBenefit = getSecondBenefit(payeRootData, benefit.benefit, removeBenefitData.removeCar)

    val benefits = benefit.benefits ++ Seq(secondBenefit).filter(_.isDefined).map(_.get)

    val (aggregateSumOfRevisedBenefitAmounts, apportionedValues) = benefits.foldLeft(BigDecimal(0), Map[String, BigDecimal]())((runningAmounts, benefit) => {
      val revisedAmount = benefit.benefitType match {
        case FUEL if differentDateForFuel(removeBenefitData.fuelDateChoice) => calculateRevisedAmount(benefit, removeBenefitData.fuelWithdrawDate.get)
        case _ => calculateRevisedAmount(benefit, removeBenefitData.withdrawDate)
      }
      (runningAmounts._1 + (benefit.grossAmount - revisedAmount), runningAmounts._2 + (benefit.benefitType.toString -> revisedAmount))
    })

    val secondWithdrawDate = removeBenefitData.fuelWithdrawDate.getOrElse(removeBenefitData.withdrawDate)

    val benefitsInfo: Map[String, BenefitInfo] = mapBenefitsInfo(benefit.benefits(0), removeBenefitData.withdrawDate, apportionedValues) ++
      secondBenefit.map(mapBenefitsInfo(_, secondWithdrawDate, apportionedValues)).getOrElse(Nil)


    val updatedBenefit = benefit.copy(benefits = benefits, benefitsInfo = benefitsInfo)
    keyStoreService.addKeyStoreEntry(
      generateKeystoreActionId(updatedBenefit.allBenefitsToString, updatedBenefit.benefit.taxYear, updatedBenefit.benefit.employmentSequenceNumber),
      KeystoreUtils.source,
      keystoreKey,
      RemoveBenefitData(removeBenefitData.withdrawDate, apportionedValues)
    )

    mainBenefitType match {
      case CAR | FUEL => {
        Ok(remove_benefit_confirm(aggregateSumOfRevisedBenefitAmounts, updatedBenefit)(user))
      }
      case _ => Logger.error(s"Unsupported type of the main benefit: $mainBenefitType, redirecting to car benefit homepage"); Redirect(routes.CarBenefitHomeController.carBenefitHome())
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
    val pathIncludingStartDate = benefit.calculations.get(payeConnector.calculationWithdrawKey()).getOrElse("")

    val benefitStartDate = dateRegex.findFirstIn(pathIncludingStartDate) map {
      dateFormat.parseLocalDate
    }

    benefitStartDate match {
      case Some(dateOfBenefitStart) if dateOfBenefitStart.isAfter(startOfCurrentTaxYear) => dateOfBenefitStart
      case _ => startOfCurrentTaxYear
    }
  }

  private[paye] val confirmBenefitRemovalAction: (User, Request[_], String, Int, Int) => Future[SimpleResult] = RemoveBenefitFlow {
    (user, request, displayBenefit, payeRootData) =>
      future {
        val payeRoot = user.regimes.paye.get
        implicit def hc = HeaderCarrier(request)
        if (carRemovalMissesFuelRemoval(payeRootData, displayBenefit)) {
          BadRequest
        } else {
          loadFormDataFor(user, displayBenefit) match {
            case Some(formData) => {
              val uri = displayBenefit.benefit.actions.getOrElse("remove",
                throw new IllegalArgumentException(s"No remove action uri found for benefit type ${displayBenefit.allBenefitsToString}"))

              val revisedBenefits = displayBenefit.benefits.map(b =>
                RevisedBenefit(b, formData.revisedAmounts.getOrElse(b.benefitType.toString,
                  throw new IllegalArgumentException(s"Unknown revised amount for benefit ${b.benefitType}"))))

              val removeBenefitResponse = payeConnector.removeBenefits(uri, payeRoot.version, revisedBenefits, formData.withdrawDate).get
              Redirect(routes.RemoveBenefitController.benefitRemoved(displayBenefit.allBenefitsToString,
                displayBenefit.benefit.taxYear, displayBenefit.benefit.employmentSequenceNumber, removeBenefitResponse.transaction.oid,
                removeBenefitResponse.calculatedTaxCode, removeBenefitResponse.personalAllowance))
            }
            case _ => Logger.error(s"Cannot find keystore entry for user ${user.oid}, redirecting to car benefit homepage"); Redirect(routes.CarBenefitHomeController.carBenefitHome())
          }
        }
      }
  }


  private[paye] val benefitRemovedAction: (User, Request[_], String, Int, Int, String, Option[String], Option[Int]) =>
    Future[SimpleResult] = (user, request, kinds, year, employmentSequenceNumber, oid, newTaxCode, personalAllowance) => {
    implicit def hc = HeaderCarrier(request)

    if (txQueueConnector.transaction(oid, user.regimes.paye.get).isEmpty) {
      Future.successful(NotFound)
    } else {
      keyStoreService.deleteKeyStore(
        generateKeystoreActionId(kinds, year, employmentSequenceNumber),
        KeystoreUtils.source
      )
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


  private def carRemovalMissesFuelRemoval(payeRootData: TaxYearData, displayBenefit: DisplayBenefit) = {
    displayBenefit.benefits.exists(_.benefitType == CAR) && !displayBenefit.benefits.exists(_.benefitType == FUEL) && hasUnremovedFuelBenefit(payeRootData, displayBenefit.benefit.employmentSequenceNumber)
  }

  private def updateBenefitForm(benefitStartDate: LocalDate,
                                carBenefitWithUnremovedFuelBenefit: Boolean,
                                dates: Option[CarFuelBenefitDates]) = Form[RemoveBenefitFormData](
    mapping(
      "withdrawDate" -> localDateMapping(Some(benefitStartDate), now().toLocalDate),
      "removeCar" -> boolean,
      "fuelRadio" -> validateFuelDateChoice(carBenefitWithUnremovedFuelBenefit),
      "fuelWithdrawDate" -> validateFuelDate(dates, Some(benefitStartDate))
    )(RemoveBenefitFormData.apply)(RemoveBenefitFormData.unapply)
  )

  private def datesForm() = Form[CarFuelBenefitDates](
    mapping(
      "withdrawDate" -> dateTuple(validate = false),
      "fuelRadio" -> optional(text)
    )(CarFuelBenefitDates.apply)(CarFuelBenefitDates.unapply)
  )

  private def getCarFuelBenefitDates(request: Request[_]): Option[CarFuelBenefitDates] = {
    datesForm().bindFromRequest()(request).value
  }

  private def calculateRevisedAmount(benefit: Benefit, withdrawDate: LocalDate)(implicit hc: HeaderCarrier): BigDecimal = {
    val calculationResult = payeConnector.calculateWithdrawBenefit(benefit, withdrawDate)
    calculationResult.result(benefit.taxYear.toString)
  }

  private def hasUnremovedFuelBenefit(payeRootData: TaxYearData, employmentNumber: Int): Boolean = {
    payeRootData.findExistingBenefit(employmentNumber, FUEL).isDefined
  }

  private def hasUnremovedCarBenefit(payeRootData: TaxYearData, employmentNumber: Int): Boolean = {
    payeRootData.findExistingBenefit(employmentNumber, CAR).isDefined
  }

  private def loadFormDataFor(user: User, benefit: DisplayBenefit)(implicit hc: HeaderCarrier) = {
    val actionId = generateKeystoreActionId(benefit.allBenefitsToString, benefit.benefit.taxYear, benefit.benefit.employmentSequenceNumber)
    keyStoreService.getEntry[RemoveBenefitData](actionId, KeystoreUtils.source, keystoreKey)
  }

  private def generateKeystoreActionId(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) = {
    s"RemoveBenefit:$benefitTypes:$taxYear:$employmentSequenceNumber"
  }


}

case class RemoveBenefitData(withdrawDate: LocalDate, revisedAmounts: Map[String, BigDecimal])
