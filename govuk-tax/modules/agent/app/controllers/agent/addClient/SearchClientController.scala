package controllers.agent.addClient

import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import play.api.mvc.{ Result, Request }
import views.html.agents.addClient._
import controllers.common.{ SessionTimeoutWrapper, ActionWrappers, BaseController }
import uk.gov.hmrc.utils.TaxYearResolver
import play.api.data.{Form, Forms}
import Forms._
import uk.gov.hmrc.common.microservice.domain.User
import models.agent.ClientSearch
import scala.Some

class SearchClientController extends BaseController with ActionWrappers with SessionTimeoutWrapper {

  def start = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { user => request => homeAction(user, request) } }

  def search = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { user => request => searchAction(user, request) } }

  private[agent] val homeAction: (User, Request[_]) => Result = (user, request) => {
    Ok(search_client(TaxYearResolver.currentTaxYearYearsRange, searchForm()))
  }

  private def searchForm() = Form[ClientSearch](
    mapping(
      "nino" -> text.verifying("you must provide a nino", validateNino(_)),
      "firstName" -> text.verifying("Invalid firstname", validateLastName(_)),
      "lastName" -> text.verifying("Invalid last name", validateFirst(_)),
      "dob" -> jodaLocalDate("dd-MM-yyyy")
    )(ClientSearch.apply)(ClientSearch.unapply)
  )

  private[addClient] def validateNino(s: String) =  {
      s.matches("[[A-Z]&&[^DFIQUV]]{2} ?\\d{2} ?\\d{2} ?\\d{2} ?[A-Z]{1}")
  }

  private[addClient] def validateLastName(s: String) = s.matches("\\.+")
  private[addClient] def validateFirst(s: String) = s.matches("\\.+")

  private[agent] val searchAction: (User, Request[_]) => Result = (user, request) => {
    Ok(search_client(TaxYearResolver.currentTaxYearYearsRange, searchForm.bindFromRequest()(request)))
  }
}

