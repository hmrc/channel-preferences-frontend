package controllers.agent.registration

import play.api.data._
import controllers.common.{ActionWrappers, SessionTimeoutWrapper, BaseController}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import play.api.mvc.{ Result, Request }
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import scala.Some
import controllers.agent.registration.FormNames._
import AgentContactDetailsFormFields._
import controllers.common.validators.Validators
import controllers.common.service.MicroServices
import controllers.common.actions.MultiFormWrapper

class AgentContactDetailsController extends MicroServices with BaseController with SessionTimeoutWrapper with ActionWrappers with AgentController with Validators with MultiFormWrapper {

  private val contactForm = Form[AgentContactDetails](
    mapping(
      daytimePhoneNumber -> smallText.verifying(phoneNumberErrorKey, validateMandatoryPhoneNumber),
      mobilePhoneNumber -> smallText.verifying(phoneNumberErrorKey, validateMandatoryPhoneNumber),
      emailAddress -> smallEmail
    )(AgentContactDetails.apply)(AgentContactDetails.unapply)
  )

  def contactDetails = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime), redirectToOrigin = true) { MultiFormAction(multiFormConfig) { user => request => contactDetailsAction(user, request) } } }

  private[registration] val contactDetailsAction: ((User, Request[_]) => Result) = (user, request) => {
    val paye: PayeRoot = user.regimes.paye.get
    val form = contactForm.fill(AgentContactDetails())
    Ok(views.html.agents.registration.contact_details(form, paye))
  }

  def postContactDetails = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { MultiFormAction(multiFormConfig) { user => request => postContactDetailsAction(user, request) } } }

  private[registration] val postContactDetailsAction: ((User, Request[_]) => Result) = (user, request) => {
    contactForm.bindFromRequest()(request).fold(
      errors => {
        BadRequest(views.html.agents.registration.contact_details(errors, user.regimes.paye.get))
      },
      _ => {
        val paye: PayeRoot = user.regimes.paye.get
        var agentDetails = contactForm.bindFromRequest()(request).data
        agentDetails += ((title, paye.title), (firstName, paye.firstName), (lastName, paye.surname), (dateOfBirth, paye.dateOfBirth), (nino, paye.nino))
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
