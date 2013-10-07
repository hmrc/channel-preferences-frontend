package controllers.agent.addClient

import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import play.api.mvc.{ Result, Request }
import views.html.agents.addClient._
import controllers.common.{ SessionTimeoutWrapper, ActionWrappers, BaseController }
import play.api.data.{Form, Forms}
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
    Ok(search_client(validDobRange, searchForm(request)))
  }

  private def unValidatedSearchForm = Form[ClientSearch](
    mapping(
      "nino" -> text,
      "firstName" -> optional(text),
      "lastName" -> optional(text),
      "dob" -> dateTuple
    )(ClientSearch.apply)(ClientSearch.unapply)
  )

  private def searchForm(request: Request[_]) = Form[ClientSearch](
    mapping(
      "nino" -> text.verifying("You must provide a valid nino", validateNino(_)),
      "firstName" -> optional(text).verifying("Invalid firstname", validateLastName(_)),
      "lastName" -> optional(text).verifying("Invalid last name", validateFirstName(_)),
      "dob" -> dateTuple.verifying("Invalid date of birth", validateDob(_))
    ) (ClientSearch.apply)(ClientSearch.unapply).verifying("nino and at least two others must be filled in", (_) => atLeastTwoOptional(unValidatedSearchForm.bindFromRequest()(request).get))
  )

  private[addClient] def validateNino(s: String) =  {
      val startsWithNaughtyCharacters = List("BG", "GB", "NK", "KN", "TN", "NT", "ZZ").foldLeft(false) {
        ((found,stringVal) => found || s.startsWith(stringVal))
      }
      def validNinoFormat = s.matches("[[A-Z]&&[^DFIQUV]][[A-Z]&&[^DFIQUVO]] ?\\d{2} ?\\d{2} ?\\d{2} ?[A-Z]{1}")
      !startsWithNaughtyCharacters && validNinoFormat
  }

  def atLeastTwoOptional(clientSearchNonValidated: ClientSearch) = {
    val ClientSearch(_, firstName, lastName, dob) = clientSearchNonValidated
    val populatedOptionalFields = Seq(firstName, lastName, dob).count(_ != None)
    populatedOptionalFields >= 2
  }

  val nameRegex = """[\p{L}\s'.-]*"""

  private[addClient] def validateLastName(s: Option[String]) = s.getOrElse("").matches(nameRegex)
  private[addClient] def validateFirstName(s: Option[String]) = s.getOrElse("").matches(nameRegex)
  private[addClient] def validateDob(dobOption: Option[LocalDate]) = {
    dobOption match {
      case Some(dob) => dob.isBefore(LocalDate.now.minusYears(16).plusDays(1)) && dob.isAfter(LocalDate.now.minusYears(110).minusDays(1))
      case None => true
    }
  }

  val validDobRange = {
    val thisYear = LocalDate.now().getYear
    (thisYear - 110) to (thisYear - 16)
  }

  private[agent] val searchAction: (User, Request[_]) => Result = (user, request) => {
    val form = searchForm(request).bindFromRequest()(request)
    if (form.hasErrors) BadRequest(search_client(validDobRange, form))
    else Ok("Searching...")
  }
}

