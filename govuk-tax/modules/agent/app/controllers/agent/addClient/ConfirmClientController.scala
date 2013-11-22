package controllers.agent.addClient

import controllers.common._
import controllers.common.validators.Validators
import uk.gov.hmrc.common.microservice.agent.AgentRegime
import play.api.mvc.Request
import views.html.agents.addClient.{preferred_contact, search_client_result}
import SearchClientController.KeyStoreKeys._
import play.api.data.Form
import models.agent.addClient.{PreferredContactData, ConfirmClient}
import play.api.data.Forms._
import PreferredClientController.emptyUnValidatedPreferredContactForm
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import models.agent.addClient.PotentialClient
import scala.Some
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.actions.{HeaderCarrier, Actions}

class ConfirmClientController(keyStoreConnector: KeyStoreConnector,
                              override val auditConnector: AuditConnector)
                             (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with Validators {

  def this() = this(Connectors.keyStoreConnector, Connectors.auditConnector)(Connectors.authConnector)

  import ConfirmClientController._

  def confirm = AuthorisedFor(AgentRegime) { user => implicit request =>
    confirmAction(user)
  }

  private[agent] def confirmAction(user: User)(implicit request: Request[_]): SimpleResult = {
    val form = confirmClientForm().bindFromRequest()(request)
    keyStoreConnector.getEntry[PotentialClient](keystoreId(user.oid, form(FieldIds.instanceId).value.getOrElse("instanceIdNotFound")), serviceSourceKey, addClientKey) match {
      case Some(potentialClient@PotentialClient(Some(searchedClient), _, _)) => {
        form.fold(
          errors => BadRequest(search_client_result(searchedClient, form)),
          confirmationWithInstanceId => {
            val (confirmation, instanceId) = confirmationWithInstanceId

            keyStoreConnector.addKeyStoreEntry(
              id = keystoreId(user.oid, instanceId),
              source = serviceSourceKey,
              key = addClientKey,
              data = potentialClient.copy(confirmation = Some(confirmation.copy(internalClientReference = trimmed(confirmation.internalClientReference)))))

            Ok(preferred_contact(emptyUnValidatedPreferredContactForm().fill((PreferredContactData.empty, instanceId))))
          }
        )
      }
      case _ =>
        Redirect(routes.SearchClientController.restart())
    }
  }
}

object ConfirmClientController {

  private[addClient] def confirmClientForm() = Form(
    mapping(
      FieldIds.correctClient -> checked("error.agent.addClient.confirmClient.correctClient"),
      FieldIds.authorised -> checked("error.agent.addClient.confirmClient.authorised"),
      FieldIds.internalClientRef -> optional(text),
      FieldIds.instanceId -> nonEmptyText
    )((correctClient, authorised, internalClientRef, instanceId) => (ConfirmClient(correctClient, authorised, internalClientRef), instanceId))
      (confirmClientWithInstanceId => Some((confirmClientWithInstanceId._1.correctClient, confirmClientWithInstanceId._1.authorised, confirmClientWithInstanceId._1.internalClientReference, confirmClientWithInstanceId._2)))
  )

  object FieldIds {
    val correctClient = "correctClient"
    val authorised = "authorised"
    val internalClientRef = "internalClientReference"
    val instanceId = "instanceId"
  }

  def trimmed(original: Option[String]): Option[String] = original.filter(!_.trim().isEmpty)
}
