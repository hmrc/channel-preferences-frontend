package controllers.agent.addClient

import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import play.api.mvc.{ Result, Request }
import views.html.agents.addClient._
import controllers.common.{ SessionTimeoutWrapper, ActionWrappers, BaseController }
import uk.gov.hmrc.utils.TaxYearResolver
import play.api.data.{Mapping, Form, Forms}
import Forms._
import uk.gov.hmrc.common.microservice.domain.User
import models.agent.ClientSearch
import scala.Some
import org.joda.time.LocalDate
import controllers.common.validators.Validators

class SearchClientController extends BaseController with ActionWrappers with SessionTimeoutWrapper with Validators {

  def start = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { user => request => homeAction(user, request) } }

  def search = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { user => request => searchAction(user, request) } }

  private[agent] val homeAction: (User, Request[_]) => Result = (user, request) => {
    Ok(search_client(validDobRange, searchForm()))
  }

  private def searchForm() = Form[ClientSearch](
    mapping(
      "nino" -> text.verifying("You must provide a valid nino", validateNino(_)),
      "firstName" -> text.verifying("Invalid firstname", validateLastName(_)),
      "lastName" -> text.verifying("Invalid last name", validateFirst(_)),
      "dob" -> dateTuple.verifying("Invalid date of birth", validateDob(_))
    )(ClientSearch.apply)(ClientSearch.unapply)
  )

  private[addClient] def validateNino(s: String) =  {
      val startsWithNaughtyCharacters = List("BG", "GB", "NK", "KN", "TN", "NT", "ZZ").foldLeft(false) {
        ((found,stringVal) => found || s.startsWith(stringVal))
      }
      def validNinoFormat = s.matches("[[A-Z]&&[^DFIQUV]][[A-Z]&&[^DFIQUVO]] ?\\d{2} ?\\d{2} ?\\d{2} ?[A-Z]{1}")
      !startsWithNaughtyCharacters && validNinoFormat
  }



  private[addClient] def validateLastName(s: String) = s.matches("""\w+""")
  private[addClient] def validateFirst(s: String) = s.matches("""\w+""")
  private[addClient] def validateDob(dobOption: Option[LocalDate]) = {
    dobOption match {
      case Some(dob) => dob.isBefore(LocalDate.now.minusYears(16).plusDays(1)) && dob.isAfter(LocalDate.now.minusYears(110).minusDays(1))
      case _ => false
    }
  }

  val validDobRange = {
    val thisYear = LocalDate.now().getYear
    (thisYear - 110) to (thisYear - 16)
  }

  private[agent] val searchAction: (User, Request[_]) => Result = (user, request) => {
    Ok(search_client(validDobRange, searchForm.bindFromRequest()(request)))
  }
}

