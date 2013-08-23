package controllers.agent.registration

import controllers.common.{ ActionWrappers, SessionTimeoutWrapper, BaseController }
import uk.gov.hmrc.microservice.paye.domain.PayeRegime
import play.api.data.Form
import play.api.data.Forms._
import scala.Some

class AgentTypeAndLegalEntityController extends BaseController with SessionTimeoutWrapper with ActionWrappers with MultiformRegistration {

  val configuration = new Configuration()

  private val agentTypeAndLegalEntityForm = Form[AgentTypeAndLegalEntity](
    mapping(
      "agentType" -> nonEmptyText.verifying("error.illegal.value", v => { configuration.agentTypeOptions.contains(v) }),
      "legalEntity" -> nonEmptyText.verifying("error.illegal.value", v => { configuration.agentTypeOptions.contains(v) })
    )(AgentTypeAndLegalEntity.apply)(AgentTypeAndLegalEntity.unapply)
  )

  def agentType = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(Some(PayeRegime)) {
      user =>
        implicit request =>
          Ok(views.html.agents.registration.agent_type_and_legal_entity(agentTypeAndLegalEntityForm, configuration))
    }
  }

  def postDetails = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(Some(PayeRegime)) {
      user =>
        implicit request =>
          agentTypeAndLegalEntityForm.bindFromRequest.fold(
            errors => {
              BadRequest(views.html.agents.registration.agent_type_and_legal_entity(errors, configuration))
            },
            _ => {
              val agentTypeAndLegalEntityDetails = agentTypeAndLegalEntityForm.bindFromRequest.data
              saveFormToKeyStore("agentTypeAndLegalEntityForm", agentTypeAndLegalEntityDetails)
              Redirect(routes.AgentTypeAndLegalEntityController.agentType)
            }
          )
    }
  }
}
case class AgentTypeAndLegalEntity(agentType: String, legalEntity: String)

case class Configuration(
  agentTypeOptions: Map[String, String] = Map[String, String](
    "inBusiness" -> "In business as an agent",
    "unpaidAgentFamily" -> "Unpaid agent - Friends and family",
    "unpaidAgentVoluntary" -> "Unpaid agent - Voluntary and Community Sector",
    "employer" -> "Employer acting for employees"
  ),
  legalEntityOptions: Map[String, String] = Map[String, String](
    "ltdCompany" -> "Limited Company",
    "partnership" -> "Partnership (e.g. Ordinary Partnership, Limited Partnership, Limited Liability Partnership, Scottish Limited Partnership)",
    "soleProprietor" -> "Sole Proprietor"
  ))

