package controllers.agent.registration

import scala.Some
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import play.api.mvc.{SimpleResult, Request}
import controllers.agent.registration.FormNames._
import AgentCompanyDetailsFormFields._
import play.api.data.Forms._
import play.api.data._
import controllers.common.validators.Validators
import uk.gov.hmrc.common.microservice.domain.Address
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.BaseController
import controllers.common.actions.{HeaderCarrier, Actions, MultiFormWrapper}
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import controllers.agent.AgentsRegimeRoots

class AgentCompanyDetailsController(override val auditConnector: AuditConnector,
                                    override val keyStoreConnector: KeyStoreConnector)
                                   (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with AgentController
  with Validators
  with MultiFormWrapper
  with AgentsRegimeRoots {

  def this() = this(Connectors.auditConnector, Connectors.keyStoreConnector)(Connectors.authConnector)

  def companyDetails = AuthorisedFor(PayeRegime) {
    MultiFormAction(multiFormConfig) {
      user => request => companyDetailsAction(user, request)
    }
  }

  def postCompanyDetails = AuthorisedFor(PayeRegime) {
    MultiFormAction(multiFormConfig) {
      user => request => postCompanyDetailsAction(user, request)
    }
  }

  private val companyDetailsForm = Form[AgentCompanyDetails](
    mapping(
      companyName -> nonEmptyNotBlankSmallText,
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
      registeredOnHMRC -> boolean.verifying("error.agent.companyDetails.registered", e => {
        e
      })
    ) {
      (companyName, tradingName, phoneNumbers, website, email, mainAddress, communicationAddress, businessAddress, saUtr,
       ctUtr, vatVrn, payeEmpRef, companyHouseNumber, registeredOnHMRC) =>
        AgentCompanyDetails(
          companyName = companyName,
          tradingName = tradingName,
          landlineNumber = phoneNumbers._1,
          mobileNumber = phoneNumbers._2,
          website = website, email = email,
          mainAddress = Address(
            addressLine1 = mainAddress._1,
            addressLine2 = mainAddress._2,
            addressLine3 = mainAddress._3,
            addressLine4 = mainAddress._4,
            postcode = mainAddress._5),
          communicationAddress = Address(
            addressLine1 = communicationAddress._1,
            addressLine2 = communicationAddress._2,
            addressLine3 = communicationAddress._3,
            addressLine4 = communicationAddress._4,
            postcode = communicationAddress._5),
          businessAddress = Address(
            addressLine1 = businessAddress._1,
            addressLine2 = businessAddress._2,
            addressLine3 = businessAddress._3,
            addressLine4 = businessAddress._4,
            postcode = businessAddress._5),
          saUtr = saUtr,
          ctUtr = ctUtr,
          vatVrn = vatVrn,
          payeEmpRef = payeEmpRef,
          companyHouseNumber = companyHouseNumber,
          registeredOnHMRC = registeredOnHMRC)
    } {
      form =>
        Some(
          (form.companyName,
            form.tradingName,
            (form.landlineNumber, form.mobileNumber),
            form.website,
            form.email,
            (form.mainAddress.addressLine1, form.mainAddress.addressLine2, form.mainAddress.addressLine3, form.mainAddress.addressLine4, form.mainAddress.postcode),
            (form.communicationAddress.addressLine1, form.communicationAddress.addressLine2, form.communicationAddress.addressLine3, form.communicationAddress.addressLine4, form.communicationAddress.postcode),
            (form.businessAddress.addressLine1, form.businessAddress.addressLine2, form.businessAddress.addressLine3, form.businessAddress.addressLine4, form.businessAddress.postcode),
            form.saUtr,
            form.ctUtr,
            form.vatVrn,
            form.payeEmpRef,
            form.companyHouseNumber,
            form.registeredOnHMRC)
        )
    }
  )

  private[registration] val companyDetailsAction: ((User, Request[_]) => SimpleResult) = (user, request) =>
    Ok(views.html.agents.registration.company_details(companyDetailsForm.fill(AgentCompanyDetails())))


  private[registration] val postCompanyDetailsAction: ((User, Request[_]) => SimpleResult) = (user, request) => {
    companyDetailsForm.bindFromRequest()(request).fold(
      errors => {
        BadRequest(views.html.agents.registration.company_details(errors))
      },
      _ => {
        val agentCompanyDetails = companyDetailsForm.bindFromRequest()(request).data
        implicit val hc = HeaderCarrier(request)
        keyStoreConnector.addKeyStoreEntry(actionId(), agent, companyDetailsFormName, agentCompanyDetails, true)
        Redirect(routes.AgentProfessionalBodyMembershipController.professionalBodyMembership())
      }
    )
  }

  def step: String = companyDetailsFormName
}

case class AgentCompanyDetails(companyName: String = "",
                               tradingName: Option[String] = None,
                               landlineNumber: Option[String] = None,
                               mobileNumber: Option[String] = None,
                               website: Option[String] = None,
                               email: String = "",
                               mainAddress: Address = Address(),
                               communicationAddress: Address = Address(),
                               businessAddress: Address = Address(),
                               saUtr: String = "",
                               ctUtr: Option[String] = None,
                               vatVrn: Option[String] = None,
                               payeEmpRef: Option[String] = None,
                               companyHouseNumber: Option[String] = None,
                               registeredOnHMRC: Boolean = false)

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