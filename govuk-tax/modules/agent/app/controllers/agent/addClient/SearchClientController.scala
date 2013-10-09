package controllers.agent.addClient

import play.api.mvc.{ Result, Request }
import views.html.agents.addClient._
import controllers.common.{ SessionTimeoutWrapper, ActionWrappers, BaseController }
import play.api.data.{Form, Forms}
import Forms._
import org.joda.time.LocalDate
import controllers.common.validators.Validators
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import models.agent.addClient.ClientSearch
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.service.MicroServices
import uk.gov.hmrc.common.microservice.agent.{MatchingPerson, SearchRequest}
import uk.gov.hmrc.utils.DateConverter
import uk.gov.hmrc.common.microservice.agent.AgentRegime
import ConfirmClientController.addClientForm

class SearchClientController(keyStore: KeyStoreMicroService) extends BaseController
                                                                with ActionWrappers
                                                                with SessionTimeoutWrapper
                                                                with Validators {
  import SearchClientController.Validation._
  import SearchClientController.KeyStoreKeys._
  import SearchClientController.FieldIds._

  def this() = this(MicroServices.keyStoreMicroService)

  def start = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(AgentRegime)) { homeAction } }
  def search = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(AgentRegime)) { searchAction } }

  private[agent] def homeAction(user: User)(request: Request[_]): Result = {
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
      nino -> text.verifying("You must provide a valid nino", validateNino _),
      firstName -> optional(text).verifying("Invalid firstname", validateName _),
      lastName -> optional(text).verifying("Invalid last name", validateName _),
      dob -> dateTuple.verifying("Invalid date of birth", validateDob)
    ) (ClientSearch.apply)(ClientSearch.unapply).verifying("nino and at least two others must be filled in", (_) => atLeastTwoOptionalAndAllMandatory(unValidatedSearchForm.bindFromRequest()(request).get))
  )

  val validDobRange = {
    val thisYear = LocalDate.now().getYear
    (thisYear - 110) to (thisYear - 16)
  }

  private[agent] def searchAction(user: User)(request: Request[_]): Result = {
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

  private[addClient] object KeyStoreKeys {
    val serviceSourceKey = "agentFrontEnd"
    def keystoreId(id: String) = s"AddClient:$id"
    val clientSearchObjectKey = "clientSearchObject"
  }

  private[addClient] object FieldIds {
    val nino = "nino";
    val firstName = "firstName";
    val lastName = "lastName";
    val dob = "dob";
  }
}

