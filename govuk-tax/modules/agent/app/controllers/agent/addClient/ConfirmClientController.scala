package controllers.agent.addClient

import controllers.common.{SessionTimeoutWrapper, ActionWrappers, BaseController}
import controllers.common.validators.Validators
import uk.gov.hmrc.common.microservice.agent.AgentRegime
import play.api.mvc.{Result, Request}
import views.html.agents.addClient.{preferred_contact, search_client_result}
import SearchClientController.KeyStoreKeys._
import play.api.data.Form
import models.agent.addClient.{PotentialClient, ConfirmClient}
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.domain.User
import scala.Some
import PreferredClientController.preferredContactForm

class ConfirmClientController extends BaseController
                                 with ActionWrappers
                                 with SessionTimeoutWrapper
                                 with Validators {
  import ConfirmClientController._

  def confirm = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(AgentRegime)) { confirmAction } }

  private[agent] def confirmAction(user: User)(request: Request[_]): Result = {
    keyStoreMicroService.getEntry[PotentialClient](keystoreId(user.oid), serviceSourceKey, addClientKey) match {
      case Some(potentialClient @ PotentialClient(Some(searchedClient), _ , _ )) => {
        val form = confirmClientForm().bindFromRequest()(request)
        form.fold (
          errors => BadRequest(search_client_result(searchedClient, form)),
          confirmation => {
            keyStoreMicroService.addKeyStoreEntry(keystoreId(user.oid), serviceSourceKey, addClientKey,
              potentialClient.copy(confirmation = Some(confirmation.copy(internalClientReference = trimmed(confirmation.internalClientReference)))))
            Ok(preferred_contact(preferredContactForm(request)))
          }
        )
      }
      case _ => Redirect(routes.SearchClientController.start())
    }
  }
}
object ConfirmClientController {

  private[addClient] def confirmClientForm() = {
    Form[ConfirmClient](
      mapping(
        FieldIds.correctClient -> checked("error.agent.addClient.confirmClient.correctClient"),
        FieldIds.authorised -> checked("error.agent.addClient.confirmClient.authorised"),
        FieldIds.internalClientRef -> optional(nonEmptyText)
      )(ConfirmClient.apply)(ConfirmClient.unapply)
    )
  }
  object FieldIds {
    val correctClient = "correctClient"
    val authorised = "authorised"
    val internalClientRef = "internalClientReference"
  }

  def trimmed(original: Option[String]): Option[String] = original.filter(!_.trim().isEmpty)
}
