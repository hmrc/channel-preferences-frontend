package controllers.agent.addClient

import play.api.mvc.{SimpleResult, Request}
import views.html.agents.addClient._
import controllers.common.BaseController
import play.api.data.{Form, Forms}
import Forms._
import org.joda.time.LocalDate
import controllers.common.validators.Validators
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import models.agent.addClient.{ConfirmClient, PotentialClient, ClientSearch}
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.agent.AgentRegime
import uk.gov.hmrc.utils.DateConverter
import ConfirmClientController.confirmClientForm
import org.bson.types.ObjectId
import uk.gov.hmrc.domain.Nino
import models.agent.{SearchRequest, MatchingPerson}
import service.agent.AgentConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.actions.Actions
import controllers.agent.AgentsRegimeRoots
import scala.concurrent.Future

class SearchClientController(val keyStoreConnector: KeyStoreConnector,
                             override val auditConnector: AuditConnector)
                            (implicit agentMicroService: AgentConnector,
                             override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with AgentsRegimeRoots {

  import SearchClientController._
  import SearchClientController.KeyStoreKeys._

  def this() = this(Connectors.keyStoreConnector, Connectors.auditConnector)(AgentConnector(), Connectors.authConnector)

  def start = AuthorisedFor(account = AgentRegime, redirectToOrigin = true) {
    homeAction
  }

  def restart = AuthorisedFor(AgentRegime) {
    restartAction
  }

  def search = AuthorisedFor(AgentRegime).async { user => request =>
    searchAction(user)(request)
  }

  private[agent] def homeAction(user: User)(request: Request[_]) = startViewWith(searchForm(request))

  private[agent] def restartAction(user: User)(request: Request[_]) =
    startViewWith(searchForm(request).withGlobalError("Your saved progress has timed out. Please restart your search"))

  private def startViewWith(form: Form[(ClientSearch, String)]) =
    Ok(search_client(form.fill((ClientSearch.empty, ObjectId.get().toString))))

  private[agent] def searchAction(user: User)(implicit request: Request[_]): Future[SimpleResult] = {
    val form = searchForm(request).bindFromRequest()(request)
    form.fold(
      errors => Future.successful(BadRequest(search_client(errors))),
      searchWithInstanceId => {
        val (search, instanceId) = searchWithInstanceId
        def restricted(result: MatchingPerson) = ClientSearch(result.nino,
          search.firstName.flatMap(_ => result.firstName),
          search.lastName.flatMap(_ => result.lastName),
          for (sDob <- search.dob;
               rDob <- result.dobAsLocalDate
               if sDob.isEqual(rDob)) yield rDob)
        val agentRoot = user.regimes.agent.get
        val searchDob = search.dob.map(data => DateConverter.formatToString(data))
        val searchUri: String = agentRoot.actions.get("search").getOrElse(throw new IllegalArgumentException(s"No search action uri found"))
        agentMicroService.searchClient(searchUri, SearchRequest(search.nino, search.firstName, search.lastName, searchDob)) flatMap {
          case Some(matchingPerson) => {
            user.regimes.agent.get.clients.find(_._1 == search.nino) match {
              case Some(_) => Future.successful(Ok(search_client_result(restricted(matchingPerson), confirmClientForm().fill((ConfirmClient.empty, instanceId)).withGlobalError("This person is already your client"))))
              case _ => {
                val restrictedResult = restricted(matchingPerson)
                keyStoreConnector.addKeyStoreEntry(actionId(instanceId), serviceSourceKey, addClientKey, PotentialClient(Some(restrictedResult), None, None)).map {
                  _=> Ok(search_client_result(restrictedResult, confirmClientForm().fill((ConfirmClient.empty, instanceId))))
                }
              }
            }
          }
          case None => Future.successful(NotFound(search_client(form.withGlobalError("No match found"))))
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
    Form(
      mapping(
        nino -> text.verifying("error.agent.addClient.search.nino", validateNino _),
        firstName -> optional(text).verifying("error.agent.addClient.search.firstname", validateName _),
        lastName -> optional(text).verifying("error.agent.addClient.search.lastname", validateName _),
        dob -> dateTuple.verifying("error.agent.addClient.search.dob", validateDob),
        instanceId -> nonEmptyText
      )
        ((nino, firstName, lastName, dob, instanceId) => (ClientSearch(nino, firstName, lastName, dob), instanceId))
        ((c: (ClientSearch, String)) => Some((c._1.nino, c._1.firstName, c._1.lastName, c._1.dob, c._2)))
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

    def validateNino(s: String): Boolean = Nino.isValid(s)
  }

  private[addClient] object KeyStoreKeys {
    val serviceSourceKey = "agentFrontEnd"

    def actionId(instanceId: String) = s"AddClient-$instanceId"

    val addClientKey = "addClient"
  }

  private[addClient] object FieldIds {
    val nino = "nino"
    val firstName = "firstName"
    val lastName = "lastName"
    val dob = "dob"
    val instanceId = "instanceId"
  }

}

