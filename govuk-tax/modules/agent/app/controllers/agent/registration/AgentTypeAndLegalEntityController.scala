package controllers.agent.registration

import controllers.common.BaseController
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import play.api.data.Form
import play.api.data.Forms._
import controllers.agent.registration.FormNames._
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{SimpleResult, Request}
import controllers.common.validators.Validators
import controllers.common.actions.{HeaderCarrier, Actions, MultiFormWrapper}
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import controllers.agent.AgentsRegimeRoots
import scala.concurrent.Future

class AgentTypeAndLegalEntityController(override val auditConnector: AuditConnector,
                                        override val keyStoreConnector: KeyStoreConnector)
                                       (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with AgentController
  with Validators
  with MultiFormWrapper
  with AgentsRegimeRoots {

  def this() = this(Connectors.auditConnector, Connectors.keyStoreConnector)(Connectors.authConnector)

  private val agentTypeAndLegalEntityForm = Form[AgentTypeAndLegalEntity](
    mapping(
      AgentTypeAndLegalEntityFormFields.agentType -> nonEmptySmallText.verifying("error.illegal.value", v => {
        Configuration.config.agentTypeOptions.exists(_.key == v)
      }),
      AgentTypeAndLegalEntityFormFields.legalEntity -> nonEmptySmallText.verifying("error.illegal.value", v => {
        Configuration.config.legalEntityOptions.exists(_.key == v)
      })
    )(AgentTypeAndLegalEntity.apply)(AgentTypeAndLegalEntity.unapply)
  )

  def agentType = AuthorisedFor(PayeRegime).async {
    MultiFormAction(multiFormConfig) {
      user => request => agentTypeAction(user, request)
    }
  }


  def postAgentType = AuthorisedFor(PayeRegime).async {
    MultiFormAction.async(multiFormConfig) {
      user => request => postAgentTypeAction(user, request)
    }
  }

  private[registration] val agentTypeAction: (User, Request[_]) => SimpleResult = (user, request) => {
    Ok(views.html.agents.registration.agent_type_and_legal_entity(agentTypeAndLegalEntityForm, Configuration.config))
  }

  private[registration] val postAgentTypeAction: ((User, Request[_]) => Future[SimpleResult]) = (user, request) => {
    agentTypeAndLegalEntityForm.bindFromRequest()(request).fold(
      errors => {
        Future.successful(BadRequest(views.html.agents.registration.agent_type_and_legal_entity(errors, Configuration.config)))
      },
      _ => {
        implicit val hc = HeaderCarrier(request)
        val agentTypeAndLegalEntityDetails = agentTypeAndLegalEntityForm.bindFromRequest()(request).data
        keyStoreConnector.addKeyStoreEntry(actionId(), agent, agentTypeAndLegalEntityFormName, agentTypeAndLegalEntityDetails, true).map {
        _=> Redirect(routes.AgentCompanyDetailsController.companyDetails())
        }
      }
    )
  }

  def step: String = agentTypeAndLegalEntityFormName
}

case class AgentTypeAndLegalEntity(agentType: String, legalEntity: String)

object AgentTypeAndLegalEntityFormFields {
  val agentType = "agentType"
  val legalEntity = "legalEntity"
}