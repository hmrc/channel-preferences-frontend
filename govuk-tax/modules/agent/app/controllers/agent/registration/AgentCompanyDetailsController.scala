package controllers.agent.registration

import controllers.common.{ ActionWrappers, SessionTimeoutWrapper, BaseController }
import play.api.data.Form
import scala.Some
import uk.gov.hmrc.microservice.paye.domain.PayeRegime
import play.api.mvc.{ Action, Result }
import controllers.agent.registration.FormNames._
import AgentCompanyDetailsFormFields._
import play.api.data.Forms._
import play.api.data._

class AgentCompanyDetailsController extends BaseController with SessionTimeoutWrapper with ActionWrappers with MultiformRegistration with AgentMapper {

  private val companyDetailsForm: Form[AgentCompanyDetails] = Form(
    mapping(
      AgentCompanyDetailsFormFields.companyName -> nonEmptyText,
      tradingName -> optional(text),
      phoneNumbers -> tuple(
        landlineNumber -> optional(text.verifying(phoneNumberErrorKey, validateOptionalPhoneNumber)),
        mobileNumber -> optional(text.verifying(phoneNumberErrorKey, validateOptionalPhoneNumber))
      ).verifying("error.agent.companyDetails.mandatory.phone", data => data._1.isDefined || data._2.isDefined),
      website -> optional(text),
      AgentCompanyDetailsFormFields.email -> Forms.email,
      mainAddress -> nonEmptyText,
      communicationAddress -> nonEmptyText,
      businessAddress -> nonEmptyText,
      saUtr -> text.verifying("error.agent.saUtr", validateSaUtr),
      ctUtr -> optional(text),
      vatVrn -> optional(text),
      payeEmpRef -> optional(text),
      companyHouseNumber -> optional(text),
      registeredOnHMRC -> boolean.verifying("error.agent.companyDetails.registered", e => { e })
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
                saveFormToKeyStore(companyDetailsFormName, agentCompanyDetails, userId(user))
                Redirect(routes.AgentProfessionalBodyMembershipController.professionalBodyMembership)

              }
            )
      }
    }
}

case class AgentCompanyDetails(companyName: String = "", tradingName: Option[String] = None, landlineNumber: Option[String] = None, mobileNumber: Option[String] = None,
  website: Option[String] = None, email: String = "", mainAddress: String = "", communicationAddress: String = "",
  businessAddress: String = "", saUtr: String = "", ctUtr: Option[String] = None,
  vatVrn: Option[String] = None, payeEmpRef: Option[String] = None, companyHouseNumber: Option[String] = None, registeredOnHMRC: Boolean = false)

object AgentCompanyDetailsFormFields {
  val companyName = "companyName"
  val tradingName = "tradingName"
  val phoneNumbers = "phoneNumbers"
  val landlineNumber = "landlineNumber"
  val mobileNumber = "mobileNumber"
  val qualifiedMobileNumber = phoneNumbers + "." + mobileNumber
  val qualifiedLandlineNumber = phoneNumbers + "." + landlineNumber
  val website = "website"
  val email = "email"
  val mainAddress = "mainAddress"
  val communicationAddress = "communicationAddress"
  val businessAddress = "businessAddress"
  val saUtr = "saUtr"
  val ctUtr = "ctUtr"
  val vatVrn = "vatVrn"
  val payeEmpRef = "payeEmpRef"
  val companyHouseNumber = "companyHouseNumber"
  val registeredOnHMRC = "registeredOnHMRC"
}