package controllers.paye

import controllers.common.BaseController
import uk.gov.hmrc.common.microservice.paye.domain._
import models.paye.{Matchers, EmploymentView, EmploymentViews}
import play.api.{data, Logger}
import uk.gov.hmrc.utils.TaxYearResolver
import controllers.common.validators.Validators
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import controllers.common.actions.{HeaderCarrier, Actions}
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{Request, Session, SimpleResult}
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
import views.html.paye._
import models.paye.Matchers.transactions
import play.api

class CarBenefitHomeController(override val auditConnector: AuditConnector, override val authConnector: AuthConnector)
                              (implicit payeService: PayeConnector, txQueueMicroservice: TxQueueConnector) extends BaseController
with Actions
with Validators
with PayeRegimeRoots {

  private val carAndFuelBenefitTypes = Set(BenefitTypes.CAR, BenefitTypes.FUEL)

  private[paye] def currentTaxYear = TaxYearResolver.currentTaxYear

  def this() = this(Connectors.auditConnector, Connectors.authConnector)(Connectors.payeConnector, Connectors.txQueueConnector)

  def carBenefitHome = AuthorisedFor(account = PayeRegime, redirectToOrigin = true).async {
    implicit user =>
      implicit request =>
        assembleCarBenefitData(user.getPaye, currentTaxYear).map { details: RawTaxData =>
          carBenefitHomeAction(details).withSession(sessionWithNpsVersion(request.session))
        }
  }

  def cannotPlayInBeta = UnauthorisedAction { request =>
    Ok(cannot_play_in_beta())
  }

  def carBenefitHomeAction(details: RawTaxData)(implicit user: User): SimpleResult = {
    def betaFilter = details.employments.size != 1 || details.cars.filter(_.isActive).size > 1

    if (betaFilter) SeeOther(routes.CarBenefitHomeController.cannotPlayInBeta.url)
    else buildHomePageResponse(buildHomePageParams(details, carAndFuelBenefitTypes, currentTaxYear))
  }

  private[paye] def sessionWithNpsVersion(session: Session)(implicit user: User) =
    session + (("nps-version", user.getPaye.version.toString))

  private[paye] def buildHomePageResponse(params: Option[HomePageParams])(implicit user: User): SimpleResult = {
    params.map { params =>
      Ok(car_benefit_home(params))
    }.getOrElse {
      val message = s"Unable to find current employment for user ${user.oid}"
      Logger.error(message)
      InternalServerError(error_no_data_car_benefit_home(message))
    }
  }

  private[paye] def assembleCarBenefitData(payeRoot: PayeRoot, taxYear: Int)(implicit hc: HeaderCarrier): Future[RawTaxData] = {
    val f1 = payeRoot.fetchCars(taxYear)
    val f2 = payeRoot.fetchEmployments(taxYear)
    val f3 = payeRoot.fetchTransactionHistory(txQueueMicroservice)
    val f5 = payeRoot.fetchTaxCodes(taxYear)

    for {
      cars <- f1
      employments <- f2
      transactionHistory <- f3
      taxCodes <- f5
    } yield RawTaxData(taxYear, cars, employments, taxCodes, transactionHistory)
  }

  private[paye] def buildHomePageParams(details: RawTaxData, benefitTypes: Set[Int], taxYear: Int): Option[HomePageParams] = {
    val employmentViews = EmploymentViews.createEmploymentViews(details.employments,
      details.taxCodes,
      details.taxYear,
      benefitTypes,
      details.transactionHistory)

    val carBenefit = details.cars.find(_.isActive)
    val previousCars = details.cars.filterNot(_.isActive)

    details.employments.find(_.employmentType == Employment.primaryEmploymentType).map { primaryEmployment =>
      HomePageParams(carBenefit, primaryEmployment.employerName, primaryEmployment.sequenceNumber, taxYear, employmentViews, previousCars)
    }
  }
}

case class RawTaxData(taxYear: Int,
                      cars: Seq[CarAndFuel],
                      employments: Seq[Employment],
                      taxCodes: Seq[TaxCode],
                      transactionHistory: Seq[TxQueueTransaction])

case class HomePageParams(activeCarBenefit: Option[CarAndFuel],
                          employerName: Option[String],
                          employmentSequenceNumber: Int,
                          currentTaxYear: Int,
                          employmentViews: Seq[EmploymentView],
                          previousCarBenefits: Seq[CarAndFuel])