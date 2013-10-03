package controllers.agent.addClient

import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{ Result, Request }
import views.html.agents.addClient._
import controllers.common.{ SessionTimeoutWrapper, ActionWrappers, BaseController }
import uk.gov.hmrc.utils.TaxYearResolver
import models.agent.ClientSearch
import play.api.data.{Form, Forms}
import Forms._

class SearchClientController extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  def start = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { user => request => homeAction(user, request) } }

  def search = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { user => request => searchAction(user, request) } }

//  private def addClilentForm() = Form[ClientSearch](
//    mapping(
//      "dob" -> validateProvidedFrom(timeSource),
//      carUnavailable -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
//      numberOfDaysUnavailable -> validateNumberOfDaysUnavailable(carBenefitValues),
//      giveBackThisTaxYear -> validateGiveBackThisTaxYear(carBenefitValues),
//      providedTo -> validateProvidedTo(carBenefitValues),
//      listPrice -> validateListPrice,
//      employeeContributes -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
//      employeeContribution -> validateEmployeeContribution(carBenefitValues),
//      employerContributes -> optional(boolean).verifying("error.paye.answer_mandatory", data => data.isDefined),
//      employerContribution -> validateEmployerContribution(carBenefitValues)
//    )(CarBenefitData.apply)(CarBenefitData.unapply)
//  )

  private[agent] val homeAction: (User, Request[_]) => Result = (user, request) => {
   // val payeData = user.regimes.paye.get
    //val taxYear = TaxYearResolver.currentTaxYear
    //val benefits = payeData.benefits(taxYear)
    //val employments = payeData.employments(taxYear)
    //val taxCodes = payeData.taxCodes(taxYear)

    //val employmentViews: Seq[EmploymentView] =
     // EmploymentViews(employments, taxCodes, taxYear, payeData.recentAcceptedTransactions(), payeData.recentCompletedTransactions())

    Ok(search_client(TaxYearResolver.currentTaxYearYearsRange, Form[ClientSearch](
      mapping(
        "nino" -> text,
        "firstName" -> text,
        "lastName" -> text,
        "dob" -> jodaLocalDate("dd-MM-yyyy")
      )(ClientSearch.apply)(ClientSearch.unapply)
    )))
  }

  private[agent] val searchAction: (User, Request[_]) => Result = (user, request) => {
    Ok("Roger that.")
  }
}

