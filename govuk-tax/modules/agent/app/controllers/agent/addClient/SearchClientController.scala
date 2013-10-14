package controllers.agent.addClient

import play.api.mvc.{ Result, Request }
import views.html.agents.addClient._
import controllers.common.{ SessionTimeoutWrapper, ActionWrappers, BaseController }
import play.api.data.{Form, Forms}
import Forms._
import org.joda.time.LocalDate
import controllers.common.validators.Validators
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import models.agent.addClient.{ConfirmClient, PotentialClient, ClientSearch}
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.service.MicroServices
import uk.gov.hmrc.common.microservice.agent.{MatchingPerson, SearchRequest, AgentRegime}
import uk.gov.hmrc.utils.DateConverter
import ConfirmClientController.confirmClientForm
import org.bson.types.ObjectId

class SearchClientController(keyStore: KeyStoreMicroService) extends BaseController
                                                                with ActionWrappers
                                                                with SessionTimeoutWrapper {
  import SearchClientController._
  import SearchClientController.KeyStoreKeys._

  def this() = this(MicroServices.keyStoreMicroService)

  def start = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(AgentRegime)) { homeAction } }
  def search = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(AgentRegime)) { searchAction } }

  private[agent] def homeAction(user: User)(request: Request[_]): Result = {
    Ok(search_client(validDobRange, searchForm(request).fill((ClientSearch.empty, ObjectId.get().toString))))
  }

  private def validDobRange = {
    val thisYear = LocalDate.now().getYear
    (thisYear - 110) to (thisYear - 16)
  }

  private[agent] def searchAction(user: User)(request: Request[_]): Result = {
    val form = searchForm(request).bindFromRequest()(request)
    form.fold(
      errors => BadRequest(search_client(validDobRange, errors)),
      searchWithInstanceId => {
        val (search, instanceId) = searchWithInstanceId
        def restricted(person: MatchingPerson) = ClientSearch(person.nino,
                                                              search.firstName.flatMap(_ => person.firstName),
                                                              search.lastName.flatMap(_ => person.lastName),
                                                              search.dob.flatMap(_ => person.dobAsLocalDate))
        val searchDob = search.dob.map(data => DateConverter.formatToString(data))
        agentMicroService.searchClient(SearchRequest(search.nino, search.firstName, search.lastName, searchDob)) match {
          case Some(result @ MatchingPerson(_, _, _, _, false)) => {
            val restrictedResult = restricted(result)
            keyStore.addKeyStoreEntry(keystoreId(user.oid, instanceId), serviceSourceKey, addClientKey, PotentialClient(Some(restrictedResult),None, None))
            Ok(search_client_result(restrictedResult, confirmClientForm().fill(ConfirmClient.empty, instanceId)))
          }
          case Some(result @ MatchingPerson(_, _, _, _, true)) =>
            Ok(search_client_result(restricted(result), confirmClientForm().fill(ConfirmClient.empty, instanceId).withGlobalError("This person is already your client")))
          case None => NotFound(search_client(validDobRange, form.withGlobalError("No match found")))
        }
      }
    )
  }
}

object SearchClientController {

  private def searchForm(request: Request[_]) = {
    import Validators._
    import SearchClientController.Validation._
    import SearchClientController.FieldIds._
    Form (
      mapping(
        nino -> text.verifying("error.agent.addClient.search.nino", validateNino _),
        firstName -> optional(text).verifying("error.agent.addClient.search.firstname", validateName _),
        lastName -> optional(text).verifying("error.agent.addClient.search.lastname", validateName _),
        dob -> dateTuple.verifying("error.agent.addClient.search.dob", validateDob),
        instanceId -> nonEmptyText
      )
        ((nino, firstName, lastName, dob, instanceId) => (ClientSearch(nino, firstName, lastName, dob), instanceId))
        ((c: (ClientSearch, String)) => Some(c._1.nino, c._1.firstName, c._1.lastName, c._1.dob, c._2))
        .verifying("Nino and at least two others must be filled in",
        _ => atLeastTwoOptionalAndAllMandatory(unValidatedSearchForm.bindFromRequest()(request).get))
    )
  }

  private def unValidatedSearchForm = {
    import Validators._
    import SearchClientController.FieldIds._
    Form[ClientSearch](
      mapping(
        nino -> text,
        firstName -> optional(text),
        lastName -> optional(text),
        dob -> dateTuple
      )(ClientSearch.apply)(ClientSearch.unapply)
    )
  }

  private[addClient] object Validation {
    val nameRegex = """^[\p{L}\s'.-[0-9]]*"""
    val emailRegex = ".+\\@.+\\..+"
    val phoneRegex = "^\\+[1-9]{1}[0-9]{10}$"

    val validateDob: Option[LocalDate] => Boolean = {
      case Some(dob) => dob.isBefore(LocalDate.now.minusYears(16).plusDays(1)) && dob.isAfter(LocalDate.now.minusYears(110).minusDays(1))
      case None => true
    }

    private[addClient] def validateName(s: Option[String]) = s.getOrElse("").trim.matches(nameRegex)

    private[addClient] def validateEmail(s: Option[String]) = s.getOrElse("").trim.matches(emailRegex)

    private[addClient] def validatePhone(s: Option[String]) = s.getOrElse("").trim.matches(phoneRegex)

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

  private[addClient] object KeyStoreKeys {
    val serviceSourceKey = "agentFrontEnd"
    def keystoreId(userId: String, instanceId: String) = s"AddClient:$userId:$instanceId"
    val addClientKey = "addClient"
  }

  private[addClient] object FieldIds {
    val nino = "nino";
    val firstName = "firstName";
    val lastName = "lastName";
    val dob = "dob";
    val instanceId = "instanceId";
  }
}

