package controllers.paye

import controllers.common.{SessionKeys, BaseController}
import uk.gov.hmrc.common.microservice.paye.domain._
import models.paye.{EmploymentView, EmploymentViews}
import play.api.Logger
import controllers.common.validators.Validators
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import controllers.common.actions.{HeaderCarrier, Actions}
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{Action, Session, SimpleResult}
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
import views.html.paye._

class CarBenefitHomeController(override val auditConnector: AuditConnector, override val authConnector: AuthConnector)
                              (implicit payeService: PayeConnector, txQueueMicroservice: TxQueueConnector) extends BaseController
with Actions
with Validators
with PayeRegimeRoots
with TaxYearSupport {

  private val carAndFuelBenefitTypes = Set(BenefitTypes.CAR, BenefitTypes.FUEL)

  def this() = this(Connectors.auditConnector, Connectors.authConnector)(Connectors.payeConnector, Connectors.txQueueConnector)

  def landingRedirect = Action {
    Redirect(routes.CarBenefitHomeController.carBenefitHome())
  }

  def carBenefitHome = AuthorisedFor(regime = PayeRegime, redirectToOrigin = true).async {
    implicit user =>
      implicit request =>
        assembleCarBenefitData(user.getPaye, currentTaxYear).flatMap {
          details: RawTaxData =>
            user.getPaye.version.map {
              version =>
                carBenefitHomeAction(details).withSession(sessionWithNpsVersion(request.session, version))
            }
        }
  }

  def cannotPlayInBeta = AuthorisedFor(regime = PayeRegime, redirectToOrigin = true) {
    user => request =>
      Ok(cannot_play_in_beta(user))
  }

  def idaTokenRequiredInBeta = UnauthorisedAction {
    request => Ok(ida_token_required_in_beta())
  }

  def carBenefitHomeAction(details: RawTaxData)(implicit user: User): SimpleResult = {
    def betaFilter = details.employments.size != 1 || details.cars.count(_.isActive) > 1

    if (betaFilter) SeeOther(routes.CarBenefitHomeController.cannotPlayInBeta().url)
    else buildHomePageResponse(buildHomePageParams(details, carAndFuelBenefitTypes, currentTaxYear))
  }

  private[paye] def sessionWithNpsVersion(session: Session, version: Int) =
    session + (SessionKeys.npsVersion -> version.toString)

  private[paye] def buildHomePageResponse(params: Option[HomePageParams])(implicit user: User): SimpleResult = {
    params.map {
      params =>
        Ok(car_benefit_home(params))
    }.getOrElse {
      val message = s"Unable to find current/primary employment for user ${user.oid}"
      Logger.error(message)
      Ok(cannot_play_in_beta(user))
    }
  }


  private[paye] def assembleCarBenefitData(payeRoot: PayeRoot, taxYear: Int)(implicit hc: HeaderCarrier): Future[RawTaxData] = {
    val f1 = payeRoot.fetchCars(taxYear)
    val f2 = payeRoot.fetchEmployments(taxYear)
    val f5 = payeRoot.fetchTaxCodes(taxYear)

    for {
      cars <- f1
      employments <- f2
      taxCodes <- f5
    } yield RawTaxData(taxYear, cars, employments, taxCodes, Seq.empty)
  }

  private[paye] def buildHomePageParams(details: RawTaxData, benefitTypes: Set[Int], taxYear: Int): Option[HomePageParams] = {
    val employmentViews = EmploymentViews.createEmploymentViews(details.employments,
      details.taxCodes,
      details.taxYear,
      benefitTypes,
      details.transactionHistory)

    val carBenefit = details.cars.find(_.isActive)
    val previousCars = details.cars.filterNot(_.isActive).sortBy(_.dateWithdrawn.get.toDate).reverse


    val carBenefitGrossAmount = details.cars.headOption.map(c => BenefitValue(c.grossAmount))
    val fuelBenefitGrossAmount = details.cars.find(_.fuelBenefit.isDefined).flatMap(_.fuelBenefit.map(f => BenefitValue(f.grossAmount)))

    details.employments.find(_.employmentType == Employment.primaryEmploymentType).map {
      primaryEmployment =>
        HomePageParams(carBenefit, primaryEmployment.employerName, primaryEmployment.sequenceNumber, taxYear,
          employmentViews, previousCars, carBenefitGrossAmount, fuelBenefitGrossAmount)
    }
  }
}

case class RawTaxData(taxYear: Int,
                      cars: Seq[CarBenefit],
                      employments: Seq[Employment],
                      taxCodes: Seq[TaxCode],
                      transactionHistory: Seq[TxQueueTransaction])

case class HomePageParams(activeCarBenefit: Option[CarBenefit],
                          employerName: Option[String],
                          employmentSequenceNumber: Int,
                          currentTaxYear: Int,
                          employmentViews: Seq[EmploymentView],
                          previousCarBenefits: Seq[CarBenefit],
                          carGrossAmount: Option[BenefitValue],
                          fuelGrossAmount: Option[BenefitValue]) {
  val totalBenefitGrossAmount = HomePageParamsBuilder.buildTotalBenefitValue(carGrossAmount, fuelGrossAmount)
}


case class BenefitValue(taxableValue: BigDecimal) {
  val basicRateValue: BigDecimal = taxableValue * 0.2
  val higherRateValue: BigDecimal = taxableValue * 0.4
  val additionalRateValue: BigDecimal = taxableValue * 0.45
}

object HomePageParamsBuilder {

  def buildTotalBenefitValue(value1: Option[BenefitValue], value2: Option[BenefitValue]): Option[BenefitValue] = {
    (value1, value2) match {
      case (None, None) => None
      case (None, _) => None
      case (_, None) => None
      case _ => Some(BenefitValue(value1.map(_.taxableValue).getOrElse(BigDecimal(0)) + value2.map(_.taxableValue).getOrElse(BigDecimal(0))))
    }
  }
}
