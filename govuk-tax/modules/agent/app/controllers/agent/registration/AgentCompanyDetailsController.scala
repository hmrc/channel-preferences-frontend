package controllers.agent.registration

import controllers.common.{ ActionWrappers, SessionTimeoutWrapper, BaseController }
import play.api.data.Form
import play.api.data.Forms._
import scala.Some
import uk.gov.hmrc.microservice.paye.domain.PayeRegime
import play.api.mvc.{ Action, Result }

class AgentCompanyDetailsController extends BaseController with SessionTimeoutWrapper with ActionWrappers with MultiformRegistration {

  private val companyDetailsForm: Form[AgentCompanyDetails] = Form(
    mapping(
      "companyName" -> nonEmptyText,
      "tradingName" -> optional(text),
      "phoneNumbers" -> tuple(
        "landlineNumber" -> optional(text.verifying(phoneNumberErrorKey, validateOptionalPhoneNumber)),
        "mobileNumber" -> optional(text.verifying(phoneNumberErrorKey, validateOptionalPhoneNumber))
      ).verifying("error.agent.companyDetails.mandatory.phone", data => data._1.isDefined || data._2.isDefined),
      "website" -> optional(text),
      "email" -> email,
      "mainAddress" -> nonEmptyText,
      "communicationAddress" -> nonEmptyText,
      "businessAddress" -> nonEmptyText,
      "saUtr" -> text.verifying("error.agent.saUtr", validateSaUtr),
      "ctUtr" -> optional(text),
      "vatVrn" -> optional(text),
      "payeEmpRef" -> optional(text),
      "companyHouseNumber" -> optional(text),
      "registeredOnHMRC" -> boolean.verifying("error.agent.companyDetails.registered", e => { e })
    ) {
        (companyName, tradingName, phoneNumbers, website, email, mainAddress, communicationAddress, businessAddress, saUtr,
        ctUtr, vatVrn, payeEmpRef, companyHouseNumber, registeredOnHMRC) =>
          AgentCompanyDetails(companyName, tradingName, phoneNumbers._1, phoneNumbers._2, website, email, mainAddress,
            communicationAddress, businessAddress, saUtr, ctUtr, vatVrn, payeEmpRef, companyHouseNumber, registeredOnHMRC)
      } {
        form =>
          Some((form.companyName, form.tradingName, (form.landlineNumber, form.mobileNumber), form.website,
            form.email, form.mainAddress, form.communicationAddress, form.businessAddress, form.saUtr, form.ctUtr,
            form.vatVrn, form.payeEmpRef, form.companyHouseNumber, form.registeredOnHMRC))
      }
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
                saveFormToKeyStore("companyDetailsForm", agentCompanyDetails, userId(user))
                Ok("go to next step")

              }
            )
      }
    }
}

case class AgentCompanyDetails(companyName: String = "", tradingName: Option[String] = None, landlineNumber: Option[String] = None, mobileNumber: Option[String] = None,
  website: Option[String] = None, email: String = "", mainAddress: String = "", communicationAddress: String = "",
  businessAddress: String = "", saUtr: String = "", ctUtr: Option[String] = None,
  vatVrn: Option[String] = None, payeEmpRef: Option[String] = None, companyHouseNumber: Option[String] = None, registeredOnHMRC: Boolean = false)