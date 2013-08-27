package controllers.agent.registration

import controllers.common.{ ActionWrappers, SessionTimeoutWrapper, BaseController }
import uk.gov.hmrc.microservice.paye.domain.PayeRegime
import play.api.data.Form
import play.api.data.Forms._
import scala.Some

class AgentTypeAndLegalEntityController extends BaseController with SessionTimeoutWrapper with ActionWrappers with MultiformRegistration {

  private val agentTypeAndLegalEntityForm = Form[AgentTypeAndLegalEntity](
    mapping(
      "agentType" -> nonEmptyText.verifying("error.illegal.value", v => { Configuration.config.agentTypeOptions.contains(v) }),
      "legalEntity" -> nonEmptyText.verifying("error.illegal.value", v => { Configuration.config.legalEntityOptions.contains(v) })
    )(AgentTypeAndLegalEntity.apply)(AgentTypeAndLegalEntity.unapply)
  )

  def agentType = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(Some(PayeRegime)) {
      user =>
        implicit request =>
          Ok(views.html.agents.registration.agent_type_and_legal_entity(agentTypeAndLegalEntityForm, Configuration.config))
    }
  }

  def postDetails = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(Some(PayeRegime)) {
      user =>
        implicit request =>
          agentTypeAndLegalEntityForm.bindFromRequest.fold(
            errors => {
              BadRequest(views.html.agents.registration.agent_type_and_legal_entity(errors, Configuration.config))
            },
            _ => {
              val agentTypeAndLegalEntityDetails = agentTypeAndLegalEntityForm.bindFromRequest.data
              saveFormToKeyStore("agentTypeAndLegalEntityForm", agentTypeAndLegalEntityDetails, userId(user))
              Redirect(routes.AgentCompanyDetailsController.companyDetails())
            }
          )
    }
  }
}
case class AgentTypeAndLegalEntity(agentType: String, legalEntity: String)

