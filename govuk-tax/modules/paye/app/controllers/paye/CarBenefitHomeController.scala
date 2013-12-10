package controllers.paye

import controllers.common.BaseController
import uk.gov.hmrc.common.microservice.paye.domain._
import models.paye.{EmploymentView, EmploymentViews}
import play.api.Logger
import uk.gov.hmrc.utils.TaxYearResolver
import controllers.common.validators.Validators
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import controllers.common.actions.{HeaderCarrier, Actions}
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{Session, Request, AnyContent, SimpleResult}
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
import views.html.paye._

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
        assembleCarBenefitData(user.getPaye, currentTaxYear).map { details =>
          buildHomePageResponse(details.map(buildHomePageParams(_, carAndFuelBenefitTypes, currentTaxYear))).
            withSession(sessionWithNpsVersion(request.session))
        }
  }

  private[paye] def sessionWithNpsVersion(session: Session)(implicit user: User) =
    session +(("nps-version", user.getPaye.version.toString))

  private[paye] def buildHomePageResponse(params: Option[HomePageParams])(implicit user: User): SimpleResult = {
    params.map {
      params =>
        Ok(car_benefit_home(params.carBenefit, params.previousCarBenefits, params.fuelBenefit, params.employerName,
          params.sequenceNumber, params.currentTaxYear, params.employmentViews))
    }.getOrElse {
      val message = s"Unable to find current employment for user ${user.oid}"
      Logger.error(message)
      InternalServerError(error_no_data_car_benefit_home(message))
    }
  }

  private[paye] def assembleCarBenefitData(payeRoot: PayeRoot, taxYear: Int)(implicit hc: HeaderCarrier): Future[Option[CarBenefitDetails]] = {
    payeRoot.fetchTaxYearData(taxYear).flatMap { taxYearData =>
      taxYearData.findPrimaryEmployment.map { primaryEmployment =>
        retrieveHomepageData(payeRoot, taxYear).map { data =>
          CarBenefitDetails(data._1, data._2, data._3, data._4, data._5, taxYearData, primaryEmployment, taxYearData.previousCarBenefits)
        }
      }
    }
  }

  private[paye] def retrieveHomepageData(payeRoot: PayeRoot, taxYear: Int)(implicit hc: HeaderCarrier) = {
    val f1 = payeRoot.fetchRecentAcceptedTransactions
    val f2 = payeRoot.fetchRecentCompletedTransactions
    val f3 = payeRoot.fetchTaxCodes(taxYear)
    val f4 = payeRoot.fetchEmployments(taxYear) //TODO: Do we really need to get employments again?

    for {
      acceptedTransactions <- f1
      completedTransactions <- f2
      taxCodes <- f3
      employments <- f4
    } yield {
      (employments, taxYear, taxCodes, acceptedTransactions, completedTransactions)
    }
  }

  private[paye] def buildHomePageParams(details: CarBenefitDetails, benefitTypes: Set[Int], taxYear: Int): HomePageParams = {
    val employmentViews = EmploymentViews.createEmploymentViews(details.employments,
      details.taxCodes,
      details.taxYear,
      benefitTypes,
      details.acceptedTransactions,
      details.completedTransactions)

    val carBenefit = details.currentTaxYearData.findExistingBenefit(details.employment.sequenceNumber, BenefitTypes.CAR)
    val fuelBenefit = details.currentTaxYearData.findExistingBenefit(details.employment.sequenceNumber, BenefitTypes.FUEL)

    HomePageParams(carBenefit, fuelBenefit, details.employment.employerName, details.employment.sequenceNumber, taxYear, employmentViews, details.previousCarBenefits)
  }
}

private[paye] case class HomePageParams(carBenefit: Option[Benefit], fuelBenefit: Option[Benefit],
                                        employerName: Option[String], sequenceNumber: Int, currentTaxYear: Int,
                                        employmentViews: Seq[EmploymentView],
                                         previousCarBenefits: Seq[CarAndFuel])

private[paye] case class CarBenefitDetails(employments: Seq[Employment],
                                           taxYear: Int,
                                           taxCodes: Seq[TaxCode],
                                           acceptedTransactions: Seq[TxQueueTransaction],
                                           completedTransactions: Seq[TxQueueTransaction],
                                           currentTaxYearData: TaxYearData,
                                           employment: Employment,
                                           previousCarBenefits: Seq[CarAndFuel] )
