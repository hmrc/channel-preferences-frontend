package controllers.agent.registration

import play.api.data._
import controllers.common.BaseController
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import play.api.mvc.{SimpleResult, Request}
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import controllers.agent.registration.FormNames._
import AgentContactDetailsFormFields._
import controllers.common.validators.Validators
import controllers.common.actions.{Actions, MultiFormWrapper}
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector

class AgentContactDetailsController(override val auditConnector: AuditConnector,
                                    override val keyStoreConnector: KeyStoreConnector)
                                   (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with AgentController
  with Validators
  with MultiFormWrapper {

  def this() = this(Connectors.auditConnector, Connectors.keyStoreConnector)(Connectors.authConnector)

  private val contactForm = Form[AgentContactDetails](
    mapping(
      daytimePhoneNumber -> smallText.verifying(phoneNumberErrorKey, validateMandatoryPhoneNumber),
      mobilePhoneNumber -> smallText.verifying(phoneNumberErrorKey, validateMandatoryPhoneNumber),
      emailAddress -> smallEmail
    )(AgentContactDetails.apply)(AgentContactDetails.unapply)
  )

  def contactDetails = AuthorisedFor(account = PayeRegime, redirectToOrigin = true) {
    MultiFormAction(multiFormConfig) {
      user => request => contactDetailsAction(user, request)
    }
  }

  def postContactDetails = AuthorisedFor(PayeRegime) {
    MultiFormAction(multiFormConfig) {
      user => request => postContactDetailsAction(user, request)
    }
  }

  private[registration] val contactDetailsAction: ((User, Request[_]) => SimpleResult) = (user, request) => {
    val paye: PayeRoot = user.regimes.paye.get
    val form = contactForm.fill(AgentContactDetails())
    Ok(views.html.agents.registration.contact_details(form, paye))
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
        keyStoreConnector.addKeyStoreEntry(registrationId(user), agent, contactFormName, agentDetails)
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
