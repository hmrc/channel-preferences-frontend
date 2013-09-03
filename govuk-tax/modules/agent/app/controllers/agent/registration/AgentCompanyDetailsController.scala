package controllers.agent.registration

import scala.Some
import uk.gov.hmrc.microservice.paye.domain.PayeRegime
import play.api.mvc.{ Request, Result }
import controllers.agent.registration.FormNames._
import AgentCompanyDetailsFormFields._
import play.api.data.Forms._
import play.api.data._
import controllers.common.validators.Validators
import uk.gov.hmrc.common.microservice.domain.Address
import uk.gov.hmrc.microservice.domain.User
import controllers.common.service.MicroServices
import controllers.common.{ ActionWrappers, SessionTimeoutWrapper, BaseController }
import controllers.common.actions.MultiFormWrapper

class AgentCompanyDetailsController extends BaseController with SessionTimeoutWrapper with ActionWrappers with AgentController with Validators with MicroServices with MultiFormWrapper {

  private val companyDetailsForm = Form[AgentCompanyDetails](
    mapping(
      AgentCompanyDetailsFormFields.companyName -> nonEmptyNotBlankSmallText,
      tradingName -> optional(smallText),
      phoneNumbers -> tuple(
        landlineNumber -> optional(smallText.verifying(phoneNumberErrorKey, validateOptionalPhoneNumber)),
        mobileNumber -> optional(smallText.verifying(phoneNumberErrorKey, validateOptionalPhoneNumber))
      ).verifying("error.agent.companyDetails.mandatory.phone", data => data._1.isDefined || data._2.isDefined),
      website -> optional(smallText),
      AgentCompanyDetailsFormFields.email -> smallEmail,
      mainAddress -> addressTuple,
      communicationAddress -> addressTuple,
      businessAddress -> addressTuple,
      saUtr -> smallText.verifying("error.agent.saUtr", validateSaUtr),
      ctUtr -> optional(smallText),
      vatVrn -> optional(smallText),
      payeEmpRef -> optional(smallText),
      companyHouseNumber -> optional(smallText),
      registeredOnHMRC -> boolean.verifying("error.agent.companyDetails.registered", e => { e })
    ) {
        (companyName, tradingName, phoneNumbers, website, email, mainAddress, communicationAddress, businessAddress, saUtr,
        ctUtr, vatVrn, payeEmpRef, companyHouseNumber, registeredOnHMRC) =>
          AgentCompanyDetails(companyName, tradingName, phoneNumbers._1, phoneNumbers._2, website, email,
            new Address(mainAddress._1, mainAddress._2, mainAddress._3, mainAddress._4, mainAddress._5),
            new Address(communicationAddress._1, communicationAddress._2, communicationAddress._3, communicationAddress._4, communicationAddress._5),
            new Address(businessAddress._1, businessAddress._2, businessAddress._3, businessAddress._4, businessAddress._5),
            saUtr, ctUtr, vatVrn, payeEmpRef, companyHouseNumber, registeredOnHMRC)
      } {
        form =>
          Some((form.companyName, form.tradingName, (form.landlineNumber, form.mobileNumber), form.website, form.email,
            (form.mainAddress.addressLine1, form.mainAddress.addressLine2, form.mainAddress.addressLine3, form.mainAddress.addressLine4, form.mainAddress.postcode),
            (form.communicationAddress.addressLine1, form.communicationAddress.addressLine2, form.communicationAddress.addressLine3, form.communicationAddress.addressLine4, form.communicationAddress.postcode),
            (form.businessAddress.addressLine1, form.businessAddress.addressLine2, form.businessAddress.addressLine3, form.businessAddress.addressLine4, form.businessAddress.postcode),
            form.saUtr, form.ctUtr,
            form.vatVrn, form.payeEmpRef, form.companyHouseNumber, form.registeredOnHMRC))
      }
  )

  def companyDetails = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { MultiFormAction(multiFormConfig) { user => request => companyDetailsAction(user, request) } } }

  private[registration] val companyDetailsAction: ((User, Request[_]) => Result) = (user, request) => {
    val form = companyDetailsForm.fill(AgentCompanyDetails())
    Ok(views.html.agents.registration.company_details(form))
  }

  def postCompanyDetails = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { MultiFormAction(multiFormConfig) { user => request => postCompanyDetailsAction(user, request) } } }

  private[registration] val postCompanyDetailsAction: ((User, Request[_]) => Result) = (user, request) => {
    companyDetailsForm.bindFromRequest()(request).fold(
      errors => {
        BadRequest(views.html.agents.registration.company_details(errors))
      },
      _ => {
        val agentCompanyDetails = companyDetailsForm.bindFromRequest()(request).data
        keyStoreMicroService.addKeyStoreEntry(registrationId(user), agent, companyDetailsFormName, agentCompanyDetails)
        Redirect(routes.AgentProfessionalBodyMembershipController.professionalBodyMembership())
      }
    )
  }

  def step: String = companyDetailsFormName
}

case class AgentCompanyDetails(companyName: String = "", tradingName: Option[String] = None, landlineNumber: Option[String] = None, mobileNumber: Option[String] = None,
  website: Option[String] = None, email: String = "", mainAddress: Address = new Address(), communicationAddress: Address = new Address(),
  businessAddress: Address = new Address(), saUtr: String = "", ctUtr: Option[String] = None,
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