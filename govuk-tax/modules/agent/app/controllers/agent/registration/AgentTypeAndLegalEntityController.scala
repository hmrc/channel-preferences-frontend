package controllers.agent.registration

import controllers.common.{BaseController2, Actions}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import play.api.data.Form
import play.api.data.Forms._
import scala.Some
import controllers.agent.registration.FormNames._
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{SimpleResult, Request}
import controllers.common.validators.Validators
import controllers.common.actions.MultiFormWrapper
import controllers.common.service.MicroServices
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import uk.gov.hmrc.common.microservice.audit.AuditMicroService
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService

class AgentTypeAndLegalEntityController(override val auditMicroService: AuditMicroService,
                                        override val keyStoreMicroService: KeyStoreMicroService)
                                       (implicit override val authMicroService: AuthMicroService)
  extends BaseController2
  with Actions
  with AgentController
  with Validators
  with MultiFormWrapper {

  def this() = this(MicroServices.auditMicroService, MicroServices.keyStoreMicroService)(MicroServices.authMicroService)

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

  def agentType = ActionAuthorisedBy(Ida)(Some(PayeRegime)) {
    MultiFormAction(multiFormConfig) {
      user => request => agentTypeAction(user, request)
    }
  }

  private[registration] val agentTypeAction: (User, Request[_]) => SimpleResult = (user, request) => {
    Ok(views.html.agents.registration.agent_type_and_legal_entity(agentTypeAndLegalEntityForm, Configuration.config))
  }

  def postAgentType = ActionAuthorisedBy(Ida)(Some(PayeRegime)) {
    MultiFormAction(multiFormConfig) {
      user => request => postAgentTypeAction(user, request)
    }
  }

  private[registration] val postAgentTypeAction: ((User, Request[_]) => SimpleResult) = (user, request) => {
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

  def step: String = agentTypeAndLegalEntityFormName

}

case class AgentTypeAndLegalEntity(agentType: String, legalEntity: String)

object AgentTypeAndLegalEntityFormFields {
  val agentType = "agentType"
  val legalEntity = "legalEntity"
}