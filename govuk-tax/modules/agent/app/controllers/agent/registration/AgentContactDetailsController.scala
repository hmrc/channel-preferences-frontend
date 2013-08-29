package controllers.agent.registration

import play.api.data._
import controllers.common.{ ActionWrappers, SessionTimeoutWrapper, BaseController }
import uk.gov.hmrc.microservice.paye.domain.PayeRegime
import play.api.mvc.{ Result, Request }
import play.api.data.Forms._
import uk.gov.hmrc.microservice.domain.User
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import scala.Some
import controllers.agent.registration.FormNames._
import AgentContactDetailsFormFields._
import controllers.common.validators.Validators
import controllers.common.service.MicroServices

class AgentContactDetailsController extends MicroServices with BaseController with SessionTimeoutWrapper with ActionWrappers with AgentController with Validators {

  private val contactForm = Form[AgentContactDetails](
    mapping(
      daytimePhoneNumber -> text.verifying(phoneNumberErrorKey, validateMandatoryPhoneNumber),
      mobilePhoneNumber -> text.verifying(phoneNumberErrorKey, validateMandatoryPhoneNumber),
      emailAddress -> email
    )(AgentContactDetails.apply)(AgentContactDetails.unapply)
  )

  def contactDetails = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { user => request => contactDetailsAction(user, request) } }

  private[registration] val contactDetailsAction: ((User, Request[_]) => Result) = (user, request) => {
    val paye: PayeRoot = user.regimes.paye.get
    val form = contactForm.fill(AgentContactDetails())
    Ok(views.html.agents.registration.contact_details(form, paye))
  }

  def postContactDetails = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { user => request => postContactDetailsAction(user, request) } }

  private[registration] val postContactDetailsAction: ((User, Request[_]) => Result) = (user, request) => {
    contactForm.bindFromRequest()(request).fold(
      errors => {
        BadRequest(views.html.agents.registration.contact_details(errors, user.regimes.paye.get))
      },
      _ => {
        val agentDetails = contactForm.bindFromRequest()(request).data
        keyStoreMicroService.addKeyStoreEntry(registrationId(user), agent, contactFormName, agentDetails)
        Redirect(routes.AgentTypeAndLegalEntityController.agentType)
      }
    )
  }
}
case class AgentContactDetails(daytimePhoneNumber: String = "", mobilePhoneNumber: String = "", emailAddress: String = "")

object AgentContactDetailsFormFields {
  val daytimePhoneNumber = "daytimePhoneNumber"
  val mobilePhoneNumber = "mobilePhoneNumber"
  val emailAddress = "emailAddress"
}
