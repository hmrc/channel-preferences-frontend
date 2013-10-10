package controllers.agent.addClient

import controllers.common.{SessionTimeoutWrapper, ActionWrappers, BaseController}
import controllers.common.validators.Validators
import uk.gov.hmrc.common.microservice.agent.AgentRegime
import play.api.mvc.{Result, Request}
import views.html.agents.addClient.{search_client_preferred_contact, search_client_result}
import SearchClientController.KeyStoreKeys._
import play.api.data.Form
import models.agent.addClient.{PreferredContact, ConfirmClient}
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.agent.MatchingPerson
import scala.Some
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import controllers.agent.addClient.SearchClientController

class ConfirmClientController extends BaseController
                                 with ActionWrappers
                                 with SessionTimeoutWrapper
                                 with Validators {
  import ConfirmClientController._

  def confirm = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(AgentRegime)) { confirmAction } }
  def preferredContact = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { preferredContactAction } }


  private[agent] def confirmAction(user: User)(request: Request[_]): Result = {
    keyStoreMicroService.getEntry[MatchingPerson](keystoreId(user.oid), serviceSourceKey, clientSearchObjectKey) match {
      case Some(person) => {
        val form = confirmClientForm().bindFromRequest()(request)
        form.fold (
          errors => BadRequest(search_client_result(person, form)),
          confirmation => {
            keyStoreMicroService.addKeyStoreEntry(keystoreId(user.oid), serviceSourceKey, clientSearchConfirmKey, confirmation)
            Ok(search_client_preferred_contact(preferredContactForm(request)))
          }
        )
      }
      case _ => Redirect(routes.SearchClientController.start())
    }
  }

  private def preferredContactForm(request: Request[_]) = Form[PreferredContact](
    mapping(
      "pointOfContact" -> text,
      "contactName" -> text.verifying("Valid name is required",
        verifyContactName(_, unValidatedPreferredContactForm(request).bindFromRequest()(request).get)),
      "contactPhone" -> text.verifying("Valid phone is required",
        verifyContactName(_, unValidatedPreferredContactForm(request).bindFromRequest()(request).get)),
      "contactEmail" -> text.verifying("Valid email is required",
        verifyContactEmail(_, unValidatedPreferredContactForm(request).bindFromRequest()(request).get))
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
      case "other" => name != null && !name.trim.isEmpty && SearchClientController.Validation.validateName(Some(name))
      case "notUs" => true
      case _ => false // unknown situation
    }
  }

  private[addClient] def verifyContactPhone(name:String, preferredContact:PreferredContact) = {
    preferredContact.pointOfContact match {
      case "me" => true
      case "other" => SearchClientController.Validation.validateName(Some(name))
      case "notUs" => true
      case _ => false // unknown situation
    }
  }

  private[addClient] def verifyContactEmail(email:String, preferredContact:PreferredContact) = {
    preferredContact.pointOfContact match {
      case "me" => true
      case "other" => SearchClientController.Validation.validateEmail(Some(email))
      case "notUs" => true
      case _ => false // unknown situation
    }
  }

  private[agent] def preferredContactAction(user: User)(request: Request[_]): Result = {
    keyStoreMicroService.getEntry[MatchingPerson](keystoreId(user.oid), serviceSourceKey, clientSearchObjectKey) match {
      case Some(person) => {
        val form = preferredContactForm(request).bindFromRequest()(request)
        form.fold (
          errors => BadRequest(search_client_preferred_contact(form)),
          search => Ok("you have added a client!")
        )
      }
      case _ => Redirect(routes.SearchClientController.start())
    }
  }
}

object ConfirmClientController {
  private[addClient] def confirmClientForm() = {
    Form[ConfirmClient](
      mapping(
        FieldIds.correctClient -> checked("You must check"),
        FieldIds.authorised -> checked("tou must check"),
        FieldIds.internalClientRef -> optional(text)
      )(ConfirmClient.apply)(ConfirmClient.unapply)
    )
  }
  object FieldIds {
    val correctClient = "correctClient"
    val authorised = "authorised"
    val internalClientRef = "internalClientReference"
  }
}

