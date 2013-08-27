package controllers.agent.registration

import play.api.data._
import controllers.common.{ ActionWrappers, SessionTimeoutWrapper, BaseController }
import uk.gov.hmrc.microservice.paye.domain.PayeRegime
import play.api.mvc.{ Result, Request }
import play.api.data.Forms._
import uk.gov.hmrc.microservice.domain.User
import uk.gov.hmrc.microservice.paye.domain.PayeRoot
import scala.Some

class AgentContactDetailsController extends BaseController with SessionTimeoutWrapper with ActionWrappers with MultiformRegistration {

  private val contactForm = Form[AgentContactDetails](
    mapping(
      "daytimePhoneNumber" -> text.verifying(phoneNumberErrorKey, validateMandatoryPhoneNumber),
      "mobilePhoneNumber" -> text.verifying(phoneNumberErrorKey, validateMandatoryPhoneNumber),
      "emailAddress" -> email
    )(AgentContactDetails.apply)(AgentContactDetails.unapply)
  )

  def contactDetails =
    AuthorisedForIdaAction(Some(PayeRegime)) {
      user =>
        request => {
          contactDetailsFunction(user, request)
        }
    }

  val contactDetailsFunction: (User, Request[_]) => Result = (user, request) => {
    val paye: PayeRoot = user.regimes.paye.get
    val form = contactForm.fill(AgentContactDetails())
    Ok(views.html.agents.registration.contact_details(form, paye))
  }

  def postContacts = WithSessionTimeoutValidation {
    AuthorisedForIdaAction(Some(PayeRegime)) {
      user =>
        implicit request =>
          contactForm.bindFromRequest.fold(
            errors => {
              BadRequest(views.html.agents.registration.contact_details(errors, user.regimes.paye.get))
            },
            _ => {
              val agentDetails = contactForm.bindFromRequest.data
              saveFormToKeyStore("contactForm", agentDetails, userId(user))
              Redirect(routes.AgentTypeAndLegalEntityController.agentType)
            }
          )
    }
  }
}
case class AgentContactDetails(daytimePhoneNumber: String = "", mobilePhoneNumber: String = "", emailAddress: String = "")

