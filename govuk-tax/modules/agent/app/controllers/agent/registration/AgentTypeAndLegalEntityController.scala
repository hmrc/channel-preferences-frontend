package controllers.agent.registration

import controllers.common.{ ActionWrappers, SessionTimeoutWrapper, BaseController }
import uk.gov.hmrc.microservice.paye.domain.PayeRegime
import play.api.data.Form
import play.api.data.Forms._
import scala.Some
import controllers.agent.registration.FormNames._
import uk.gov.hmrc.microservice.domain.User
import play.api.mvc.{ Result, Request }
import controllers.common.service.MicroServices

class AgentTypeAndLegalEntityController extends MicroServices with BaseController with SessionTimeoutWrapper with ActionWrappers with AgentController {

  private val agentTypeAndLegalEntityForm = Form[AgentTypeAndLegalEntity](
    mapping(
      AgentTypeAndLegalEntityFormFields.agentType -> nonEmptyText.verifying("error.illegal.value", v => { Configuration.config.agentTypeOptions.contains(v) }),
      AgentTypeAndLegalEntityFormFields.legalEntity -> nonEmptyText.verifying("error.illegal.value", v => { Configuration.config.legalEntityOptions.contains(v) })
    )(AgentTypeAndLegalEntity.apply)(AgentTypeAndLegalEntity.unapply)
  )

  def agentType = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { user => request => agentTypeAction(user, request) } }

  private[registration] val agentTypeAction: (User, Request[_]) => Result = (user, request) => {
    Ok(views.html.agents.registration.agent_type_and_legal_entity(agentTypeAndLegalEntityForm, Configuration.config))
  }

  def postAgentType = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { user => request => postAgentTypeAction(user, request) } }

  private[registration] val postAgentTypeAction: ((User, Request[_]) => Result) = (user, request) => {
    agentTypeAndLegalEntityForm.bindFromRequest()(request).fold(
      errors => {
        BadRequest(views.html.agents.registration.agent_type_and_legal_entity(errors, Configuration.config))
      },
      _ => {
        val agentTypeAndLegalEntityDetails = agentTypeAndLegalEntityForm.bindFromRequest()(request).data
        keyStoreMicroService.addKeyStoreEntry(registrationId(user), agent, agentTypeAndLegalEntityFormName, agentTypeAndLegalEntityDetails)
        Redirect(routes.AgentCompanyDetailsController.companyDetails())
      }
    )
  }
}
case class AgentTypeAndLegalEntity(agentType: String, legalEntity: String)

object AgentTypeAndLegalEntityFormFields {
  val agentType = "agentType"
  val legalEntity = "legalEntity"
}