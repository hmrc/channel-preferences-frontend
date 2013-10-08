package controllers.agent.addClient

import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import play.api.mvc.{ Result, Request }
import views.html.agents.addClient._
import controllers.common.{ SessionTimeoutWrapper, ActionWrappers, BaseController }
import play.api.data.{Form, Forms}
import Forms._
import uk.gov.hmrc.common.microservice.domain.User
import scala.Some
import org.joda.time.LocalDate
import controllers.common.validators.Validators
import models.agent.addClient.ClientSearch
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import play.api.libs.json.Json
import controllers.common.domain.Transform._
import models.agent.addClient.ClientSearch
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.service.MicroServices

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

  def start = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { user => request => homeAction(user, request) } }

  def search = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { user => request => searchAction(user, request) } }

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
      nino -> text.verifying("You must provide a valid nino", validateNino _),
      firstName -> optional(text).verifying("Invalid firstname", validateName _),
      lastName -> optional(text).verifying("Invalid last name", validateName _),
      dob -> dateTuple.verifying("Invalid date of birth", validateDob)
    )(ClientSearch.apply)(ClientSearch.unapply).verifying("nino and at least two others must be filled in", (_) => atLeastTwoOptional(unValidatedSearchForm.bindFromRequest()(request).get))
  )

  val validDobRange = {
    val thisYear = LocalDate.now().getYear
    (thisYear - 110) to (thisYear - 16)
  }
  private[agent] val searchAction: (User, Request[_]) => Result = (user, request) => {
    val form = searchForm(request).bindFromRequest()(request)
    if (form.hasErrors) BadRequest(search_client(validDobRange, form))
    else {
      //FIXME This should be the search result when the API is available...
      val clientSearchResult = form.get
      keyStore.addKeyStoreEntry(keystoreId(user.oid), serviceSourceKey, clientSearchObjectKey, clientSearchResult)
      Ok(search_client_result(clientSearchResult))
    }
  }
}
object SearchClientController {

  private[addClient] object Validation {
    val validateDob: Option[LocalDate] => Boolean = {
      case Some(dob) => dob.isBefore(LocalDate.now.minusYears(16).plusDays(1)) && dob.isAfter(LocalDate.now.minusYears(110).minusDays(1))
      case None => true
    }

    def validateName(s: Option[String]) = s.getOrElse("").matches( """[\p{L}\s'.-]*""")

    def atLeastTwoOptional(clientSearchNonValidated: ClientSearch) = {
      val ClientSearch(_, firstName, lastName, dob) = clientSearchNonValidated
      val populatedOptionalFields = Seq(firstName, lastName, dob).count(_ != None)
      populatedOptionalFields >= 2
    }

    def validateNino(s: String) =  {
      val startsWithNaughtyCharacters = List("BG", "GB", "NK", "KN", "TN", "NT", "ZZ").foldLeft(false) {
        ((found,stringVal) => found || s.startsWith(stringVal))
      }
      def validNinoFormat = s.matches("[[A-Z]&&[^DFIQUV]][[A-Z]&&[^DFIQUVO]] ?\\d{2} ?\\d{2} ?\\d{2} ?[A-Z]{1}")
      !startsWithNaughtyCharacters && validNinoFormat
    }
  }

}

