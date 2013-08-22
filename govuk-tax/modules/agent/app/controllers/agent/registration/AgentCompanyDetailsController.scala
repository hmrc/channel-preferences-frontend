package controllers.agent.registration

import controllers.common.{ ActionWrappers, SessionTimeoutWrapper, BaseController }
import play.api.data.Form
import play.api.data.Forms._
import scala.Some
import uk.gov.hmrc.microservice.paye.domain.PayeRegime
import play.api.mvc.{ Action, Result }

class AgentCompanyDetailsController extends BaseController with SessionTimeoutWrapper with ActionWrappers with MultiformRegistration {

  private val companyDetailsForm = Form[AgentCompanyDetails](
    mapping(
      "companyName" -> text,
      "tradingName" -> text,
      "landlineNumber" -> text.verifying(phoneNumberErrorKey, validateOptionalPhoneNumber),
      "mobileNumber" -> text.verifying(phoneNumberErrorKey, validateOptionalPhoneNumber),
      "website" -> text.verifying(),
      "email" -> text.verifying("error.email", validateOptionalEmail),
      "mainAddress" -> text,
      "communicationAddress" -> text,
      "businessAddress" -> text,
      "saUtr" -> text.verifying("error.agent.saUtr", validateSaUtr),
      "ctUtr" -> text,
      "vatVrn" -> text,
      "payeEmpRef" -> text,
      "companyHouseNumber" -> text,
      "registeredOnHMRC" -> text
    )(AgentCompanyDetails.apply)(AgentCompanyDetails.unapply)
  )

  def companyDetails =
    AuthorisedForIdaAction(Some(PayeRegime)) {
      user =>
        request => {
          companyDetailsFunction
        }
    }

  val companyDetailsFunction: Result = {
    val form = companyDetailsForm.fill(AgentCompanyDetails())
    Ok(views.html.agents.registration.company_details(form))
  }

  def postCompanyDetails =
    WithSessionTimeoutValidation {
      AuthorisedForIdaAction(Some(PayeRegime)) {
        user =>
          implicit request =>
            companyDetailsForm.bindFromRequest.fold(
              errors => {
                BadRequest(views.html.agents.registration.company_details(errors))
              },
              _ => {
                val agentCompanyDetails = companyDetailsForm.bindFromRequest.data
                saveFormToKeyStore("companyDetailsForm", agentCompanyDetails)
                //Redirect(routes.AgentTypeAndLegalEntityController.agentType)
                Ok("No more forms please!!!!")
              }
            )
      }
    }
}

case class AgentCompanyDetails(companyName: String = "", tradingName: String = "", landlineNumber: String = "", mobileNumber: String = "",
  website: String = "", email: String = "", mainAddress: String = "", communicationAddress: String = "",
  businessAddress: String = "", saUtr: String = "", ctUtr: String = "",
  vatVrn: String = "", payeEmpRef: String = "", companyHouseNumber: String = "", registeredOnHMRC: String = "")