package controllers.agent.addClient

import controllers.common.{SessionTimeoutWrapper, ActionWrappers, BaseController}
import controllers.common.validators.Validators
import uk.gov.hmrc.common.microservice.agent.AgentRegime
import play.api.mvc.{Result, Request}
import views.html.agents.addClient.{client_successfully_added, search_client_preferred_contact, search_client_result}
import SearchClientController.KeyStoreKeys._
import play.api.data.Form
import models.agent.addClient.{PotentialClient, PreferredContact, ConfirmClient}
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

  def confirm = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(AgentRegime)) { confirmAction } }
  def preferredContact = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { preferredContactAction } }

  private[agent] def confirmAction(user: User)(request: Request[_]): Result = {
    keyStoreMicroService.getEntry[PotentialClient](keystoreId(user.oid), serviceSourceKey, addClientKey) match {
      case Some(potentialClient) if potentialClient.clientSearch.isDefined  => {
        val form = confirmClientForm().bindFromRequest()(request)
        form.fold (
          errors => BadRequest(search_client_result(potentialClient.clientSearch.get, form)),
          confirmation => {
            keyStoreMicroService.addKeyStoreEntry(keystoreId(user.oid), serviceSourceKey, addClientKey, potentialClient.copy(confirmation = Some(confirmation)))
            Ok(search_client_preferred_contact(preferredContactForm(request)))
          }
        )
      }
      case _ => Redirect(routes.SearchClientController.start())
    }
  }

  private def preferredContactForm(request: Request[_]) = Form[PreferredContact](
    mapping(
      pointOfContact -> text,
      contactName -> text.verifying("Valid name is required",
        verifyContactName(_, unValidatedPreferredContactForm(request).bindFromRequest()(request).get)),
      contactPhone -> text.verifying("Valid phone is required",
        verifyContactPhone(_, unValidatedPreferredContactForm(request).bindFromRequest()(request).get)),
      contactEmail -> text.verifying("Valid email is required",
        verifyContactEmail(_, unValidatedPreferredContactForm(request).bindFromRequest()(request).get))
    ) (PreferredContact.apply)(PreferredContact.unapply)
  )

  private val contactMapping = mapping(
    pointOfContact -> text,
    contactName -> text,
    contactPhone -> text,
    contactEmail -> text
  )(PreferredContact.apply)(PreferredContact.unapply)

  private def unValidatedPreferredContactForm(request: Request[_]) = Form[PreferredContact](
    contactMapping
  )

  private[addClient] def verifyContactName(name:String, preferredContact:PreferredContact) = {
    preferredContact.pointOfContact match {
      case ConfirmClientController.me => true
      case ConfirmClientController.other => name != null && !name.trim.isEmpty && SearchClientController.Validation.validateName(Some(name))
      case ConfirmClientController.notUs => true
      case _ => false // unknown situation
    }
  }

  private[addClient] def verifyContactPhone(phone:String, preferredContact:PreferredContact) = {
    preferredContact.pointOfContact match {
      case ConfirmClientController.me => true
      case ConfirmClientController.other => validateMandatoryPhoneNumber(phone) //  SearchClientController.Validation.validatePhone(Some(phone))
      case ConfirmClientController.notUs => true
      case _ => false // unknown situation
    }
  }

  private[addClient] def verifyContactEmail(email:String, preferredContact:PreferredContact) = {
    preferredContact.pointOfContact match {
      case ConfirmClientController.me => true
      case ConfirmClientController.other => SearchClientController.Validation.validateEmail(Some(email))
      case ConfirmClientController.notUs => true
      case _ => false // unknown situation
    }
  }

  private[agent] def preferredContactAction(user: User)(request: Request[_]): Result = {
    keyStoreMicroService.getEntry[PotentialClient](keystoreId(user.oid), serviceSourceKey, addClientKey) match {
      case Some(potentialClient) if potentialClient.clientSearch.isDefined &&  potentialClient.confirmation.isDefined=> {
        val form = preferredContactForm(request).bindFromRequest()(request)
        form.fold (
          errors => BadRequest(search_client_preferred_contact(form)),
          search => Ok(client_successfully_added())
        )
      }
      case _ => Redirect(routes.SearchClientController.start())
    }
  }
}

object ConfirmClientController {

  private[addClient] val pointOfContact = "pointOfContact"
  private[addClient] val contactName = "contactName"
  private[addClient] val contactPhone = "contactPhone"
  private[addClient] val contactEmail = "contactEmail"

  private[addClient] val me = "me"
  private[addClient] val other = "other"
  private[addClient] val notUs = "notUs"

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

