package controllers.agent.addClient

import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import play.api.mvc.{ Result, Request }
import views.html.agents.addClient._
import controllers.common.{ SessionTimeoutWrapper, ActionWrappers, BaseController }
import play.api.data.{Form, Forms}
import Forms._
import org.joda.time.LocalDate
import controllers.common.validators.Validators
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import models.agent.addClient.{PreferredContact, AddClient, ClientSearch}
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.service.MicroServices
import uk.gov.hmrc.common.microservice.agent.{MatchingPerson, SearchRequest, AgentMicroServices}
import uk.gov.hmrc.utils.DateConverter
import uk.gov.hmrc.common.microservice.agent.AgentRegime

class SearchClientController(keyStore: KeyStoreMicroService) extends BaseController
                                                                with ActionWrappers
                                                                with SessionTimeoutWrapper
                                                                with Validators {
  import SearchClientController.Validation._

  private[addClient] val nino = "nino";
  private[addClient] val firstName = "firstName";
  private[addClient] val lastName = "lastName";
  private[addClient] val dob = "dob";

  //FIXME: move to an object
  private[addClient] val serviceSourceKey = "agentFrontEnd"
  private[addClient] def keystoreId(id: String) = s"AddClient:$id"
  private[addClient] val clientSearchObjectKey = "clientSearchObject"

  def this() = this(MicroServices.keyStoreMicroService)

  def start = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(AgentRegime)) { user => request => homeAction(user, request) } }

  def search = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(AgentRegime)) { user => request => searchAction(user, request) } }

  def add = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(AgentRegime)) { user => request => addAction(user, request) } }

  def preferredContact = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { user => request => preferredContactAction(user, request) } }

  private[agent] val homeAction: (User, Request[_]) => Result = (user, request) => {
    Ok(search_client(validDobRange, searchForm(request)))
  }

  private def unValidatedSearchForm = Form[ClientSearch](
    mapping(
      nino -> text,
      firstName -> optional(text),
      lastName -> optional(text),
      dob -> dateTuple
    )(ClientSearch.apply)(ClientSearch.unapply)
  )

  private def searchForm(request: Request[_]) = Form[ClientSearch](
    mapping(
      "nino" -> text.verifying("You must provide a valid nino", validateNino _),
      "firstName" -> optional(text).verifying("Invalid firstname", validateName _),
      "lastName" -> optional(text).verifying("Invalid last name", validateName _),
      "dob" -> dateTuple.verifying("Invalid date of birth", validateDob)
    ) (ClientSearch.apply)(ClientSearch.unapply).verifying("nino and at least two others must be filled in", (_) => atLeastTwoOptionalAndAllMandatory(unValidatedSearchForm.bindFromRequest()(request).get))
  )

  val validDobRange = {
    val thisYear = LocalDate.now().getYear
    (thisYear - 110) to (thisYear - 16)
  }
  private[agent] val searchAction: (User, Request[_]) => Result = (user, request) => {
    val form = searchForm(request).bindFromRequest()(request)
    form.fold(
      errors => BadRequest(search_client(validDobRange, errors)),
      search => {
        val searchDob = search.dob.map(data => DateConverter.formatToString(data))
        agentMicroService.searchClient(SearchRequest(search.nino, search.firstName, search.lastName, searchDob)) match {
          case Some(result) => {
            val restrictedResult = MatchingPerson(result.nino,
                                                  search.firstName.flatMap(_ => result.firstName),
                                                  search.lastName.flatMap(_ => result.lastName),
                                                  search.dob.flatMap(_ => result.dateOfBirth))
            keyStore.addKeyStoreEntry(keystoreId(user.oid), serviceSourceKey, clientSearchObjectKey, restrictedResult)
            Ok(search_client_result(restrictedResult, addClientForm(request)))
          }
          case None => NotFound(search_client(validDobRange, form.withGlobalError("No match found")))
        }
      }
    )
  }

  private def addClientForm(request: Request[_]) = Form[AddClient](
    mapping(
      "correctClient" -> checked("You must check"),
      "authorised" -> checked("tou must check"),
      "internalClientReference" -> nonEmptyText()
    ) (AddClient.apply)(AddClient.unapply)
  )

  private def preferredContactForm(request: Request[_]) = Form[PreferredContact](
    mapping(
      "pointOfContact" -> text,
      "contactName" -> text,
      "contactPhone" -> text,
      "contactEmail" -> text
    ) (PreferredContact.apply)(PreferredContact.unapply)
  )

  private[agent] val addAction: (User, Request[_]) => Result = (user, request) => {
    val searchedUser = keyStore.getEntry[MatchingPerson](keystoreId(user.oid), serviceSourceKey, clientSearchObjectKey)

    searchedUser match {
      case Some(u) => {
        val form = addClientForm(request).bindFromRequest()(request)
        if (form.hasErrors) {
          Ok(search_client_result(u, form))
        } else {
          Ok(search_client_preferred_contact(preferredContactForm(request).bindFromRequest()(request)))
        }
      }
      case None => BadRequest("Requested to add a user but none has been selected")
    }
  }

  private[agent] val preferredContactAction: (User, Request[_]) => Result = (user, request) => {
    Ok("yay")
  }
}

object SearchClientController {

  private[addClient] object Validation {
    val nameRegex = """^[\p{L}\s'.-[0-9]]*"""

    val validateDob: Option[LocalDate] => Boolean = {
      case Some(dob) => dob.isBefore(LocalDate.now.minusYears(16).plusDays(1)) && dob.isAfter(LocalDate.now.minusYears(110).minusDays(1))
      case None => true
    }

    private[addClient] def validateName(s: Option[String]) = s.getOrElse("").trim.matches(nameRegex)

    private[addClient] def atLeastTwoOptionalAndAllMandatory(clientSearchNonValidated: ClientSearch) = {
      val items = List(clientSearchNonValidated.firstName.getOrElse("").trim.length > 0,
          clientSearchNonValidated.lastName.getOrElse("").trim.length > 0,
          clientSearchNonValidated.dob.isDefined)

      val count = items.foldLeft(0)((sum, valid) => if (valid) sum + 1 else sum)
      count >= 2 && validateNino(clientSearchNonValidated.nino)
    }

    def validateNino(s: String):Boolean =  {
      if (s == null)
        return false

      val startsWithNaughtyCharacters = List("BG", "GB", "NK", "KN", "TN", "NT", "ZZ").foldLeft(false) {
        ((found,stringVal) => found || s.startsWith(stringVal))
      }
      def validNinoFormat = s.matches("[[A-Z]&&[^DFIQUV]][[A-Z]&&[^DFIQUVO]] ?\\d{2} ?\\d{2} ?\\d{2} ?[A-Z]{1}")
      !startsWithNaughtyCharacters && validNinoFormat
    }
  }

}

