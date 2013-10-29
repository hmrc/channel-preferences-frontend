package controllers.agent.registration

import play.api.data._
import controllers.common.{BaseController2, Actions}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import play.api.mvc.{SimpleResult, Request}
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import scala.Some
import controllers.agent.registration.FormNames._
import AgentContactDetailsFormFields._
import controllers.common.validators.Validators
import controllers.common.actions.MultiFormWrapper
import uk.gov.hmrc.common.microservice.auth.AuthMicroService
import uk.gov.hmrc.common.microservice.audit.AuditMicroService
import controllers.common.service.MicroServices
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService

class AgentContactDetailsController(override val auditMicroService: AuditMicroService,
                                    override val keyStoreMicroService: KeyStoreMicroService)
                                   (implicit override val authMicroService: AuthMicroService)
  extends BaseController2
  with Actions
  with AgentController
  with Validators
  with MultiFormWrapper {

  def this() = this(MicroServices.auditMicroService, MicroServices.keyStoreMicroService)(MicroServices.authMicroService)

  private val contactForm = Form[AgentContactDetails](
    mapping(
      daytimePhoneNumber -> smallText.verifying(phoneNumberErrorKey, validateMandatoryPhoneNumber),
      mobilePhoneNumber -> smallText.verifying(phoneNumberErrorKey, validateMandatoryPhoneNumber),
      emailAddress -> smallEmail
    )(AgentContactDetails.apply)(AgentContactDetails.unapply)
  )

  def contactDetails = ActionAuthorisedBy(Ida)(Some(PayeRegime), redirectToOrigin = true) {
    MultiFormAction(multiFormConfig) {
      user => request => contactDetailsAction(user, request)
    }
  }

  private[registration] val contactDetailsAction: ((User, Request[_]) => SimpleResult) = (user, request) => {
    val paye: PayeRoot = user.regimes.paye.get
    val form = contactForm.fill(AgentContactDetails())
    Ok(views.html.agents.registration.contact_details(form, paye))
  }

  def postContactDetails = ActionAuthorisedBy(Ida)(Some(PayeRegime)) {
    MultiFormAction(multiFormConfig) {
      user => request => postContactDetailsAction(user, request)
    }
  }

  private[registration] val postContactDetailsAction: ((User, Request[_]) => SimpleResult) = (user, request) => {
    contactForm.bindFromRequest()(request).fold(
      errors => {
        BadRequest(views.html.agents.registration.contact_details(errors, user.regimes.paye.get))
      },
      _ => {
        val paye: PayeRoot = user.regimes.paye.get
        var agentDetails = contactForm.bindFromRequest()(request).data
        agentDetails +=((title, paye.title), (firstName, paye.firstName), (lastName, paye.surname), (dateOfBirth, paye.dateOfBirth), (nino, paye.nino))
        keyStoreMicroService.addKeyStoreEntry(registrationId(user), agent, contactFormName, agentDetails)
        Redirect(routes.AgentTypeAndLegalEntityController.agentType())
      }
    )
  }

  def step: String = contactFormName

}

case class AgentContactDetails(daytimePhoneNumber: String = "", mobilePhoneNumber: String = "", emailAddress: String = "")

object AgentContactDetailsFormFields {
  val daytimePhoneNumber = "daytimePhoneNumber"
  val mobilePhoneNumber = "mobilePhoneNumber"
  val emailAddress = "emailAddress"
  val title = "title"
  val firstName = "firstName"
  val lastName = "lastName"
  var dateOfBirth = "dateOfBirth"
  var nino = "nino"
}
