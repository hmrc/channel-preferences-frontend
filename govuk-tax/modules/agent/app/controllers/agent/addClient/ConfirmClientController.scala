package controllers.agent.addClient

import controllers.common.{SessionTimeoutWrapper, ActionWrappers, BaseController}
import controllers.common.validators.Validators
import uk.gov.hmrc.common.microservice.agent.{MatchingPerson, AgentRegime}
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{Result, Request}
import views.html.agents.addClient.{search_client_preferred_contact, search_client_result}
import SearchClientController.KeyStoreKeys._
import play.api.data.Form
import models.agent.addClient.{PreferredContact, AddClient}
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.agent.MatchingPerson
import scala.Some
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime

class ConfirmClientController extends BaseController
                                 with ActionWrappers
                                 with SessionTimeoutWrapper
                                 with Validators {
  import ConfirmClientController._

  def confirm = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(AgentRegime)) { addAction } }
  def preferredContact = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { preferredContactAction } }


  private[agent] def addAction(user: User)(request: Request[_]): Result = {
    val searchedUser = keyStoreMicroService.getEntry[MatchingPerson](keystoreId(user.oid), serviceSourceKey, clientSearchObjectKey)

    searchedUser match {
      case Some(u) => {
        val form = addClientForm(request).bindFromRequest()(request)
        if (form.hasErrors) {
          Ok(search_client_result(u, form))
        } else {
          Ok(search_client_preferred_contact(preferredContactForm(request)))
        }
      }
      case None => BadRequest("Requested to add a user but none has been selected")
    }
  }

  private def preferredContactForm(request: Request[_]) = Form[PreferredContact](
    mapping(
      "pointOfContact" -> text,
      "contactName" -> text.verifying("Name is required",
        verifyContactName(_, unValidatedPreferredContactForm(request).bindFromRequest()(request).get)),
      "contactPhone" -> text.verifying("Phone is required",
        verifyContactName(_, unValidatedPreferredContactForm(request).bindFromRequest()(request).get)),
      "contactEmail" -> text.verifying("Email is required",
        verifyContactName(_, unValidatedPreferredContactForm(request).bindFromRequest()(request).get))
    ) (PreferredContact.apply)(PreferredContact.unapply)
  )

  private val contactMapping = mapping(
    "pointOfContact" -> text,
    "contactName" -> text,
    "contactPhone" -> text,
    "contactEmail" -> text
  )(PreferredContact.apply)(PreferredContact.unapply)

  private def unValidatedPreferredContactForm(request: Request[_]) = Form[PreferredContact](
    contactMapping
  )

  private[addClient] def verifyContactName(name:String, preferredContact:PreferredContact) = {
    preferredContact.pointOfContact match {
      case "me" => true
      case "other" => false
      case "notUs" => true
      case _ => false // unknown situation
    }
  }

  private[addClient] def verifyContactPhone(name:String, preferredContact:PreferredContact) = {
    preferredContact.pointOfContact match {
      case "me" => true
      case "other" => false
      case "notUs" => true
      case _ => false // unknown situation
    }
  }

  private[addClient] def verifyContactEmail(name:String, preferredContact:PreferredContact) = {
    preferredContact.pointOfContact match {
      case "me" => true
      case "other" => false
      case "notUs" => true
      case _ => false // unknown situation
    }
  }

  private[agent] def preferredContactAction(user: User)(request: Request[_]): Result = {
    val form = preferredContactForm(request).bindFromRequest()(request)

    if (form.hasErrors) {
      BadRequest(search_client_preferred_contact(form))
    }  else {
      Ok("you have added a client!")
    }
  }
}
object ConfirmClientController {
  private[addClient] def addClientForm(request: Request[_]) = Form[AddClient](
    mapping(
      "correctClient" -> checked("You must check"),
      "authorised" -> checked("tou must check"),
      "internalClientReference" -> nonEmptyText()
    ) (AddClient.apply)(AddClient.unapply)
  )
}
