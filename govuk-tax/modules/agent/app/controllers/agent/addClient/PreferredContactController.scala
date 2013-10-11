package controllers.agent.addClient

import controllers.common.{SessionTimeoutWrapper, ActionWrappers, BaseController}
import controllers.common.validators.Validators
import play.api.mvc.{Result, Request}
import views.html.agents.addClient.{client_successfully_added, preferred_contact}
import SearchClientController.KeyStoreKeys._
import play.api.data.Form
import play.api.data.Forms._
import controllers.agent.addClient.PreferredClientController._
import uk.gov.hmrc.common.microservice.domain.User
import models.agent.addClient.PotentialClient
import models.agent.addClient.PreferredContact
import scala.Some
import Validators.validateMandatoryPhoneNumber
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime

class PreferredContactController extends BaseController
                                 with ActionWrappers
                                 with SessionTimeoutWrapper {
  def preferredContact = WithSessionTimeoutValidation { AuthorisedForIdaAction(Some(PayeRegime)) { preferredContactAction } }

  private[agent] def preferredContactAction(user: User)(request: Request[_]): Result = {
    keyStoreMicroService.getEntry[PotentialClient](keystoreId(user.oid), serviceSourceKey, addClientKey) match {
      case Some(PotentialClient(Some(_), Some(_), _ )) => {
        val form = preferredContactForm(request).bindFromRequest()(request)
        //FIXME we should trim contact details before saving them here
        form.fold (
          errors => BadRequest(preferred_contact(form)),
          search => Ok(client_successfully_added())
        )
      }
      case _ => Redirect(routes.SearchClientController.start())
    }
  }
}
object PreferredClientController {

  object FieldIds {
    private[addClient] val pointOfContact = "pointOfContact"
    private[addClient] val contactName = "contactName"
    private[addClient] val contactPhone = "contactPhone"
    private[addClient] val contactEmail = "contactEmail"
    private[addClient] val me = "me"
    private[addClient] val other = "other"
    private[addClient] val notUs = "notUs"
  }

  private[addClient] def preferredContactForm(request: Request[_]) = {
    lazy val unvalidatedForm = unValidatedPreferredContactForm(request).bindFromRequest()(request).get
    Form[PreferredContact](
      mapping(
        FieldIds.pointOfContact -> text,
        FieldIds.contactName -> text.verifying("error.agent.addClient.contact.name", verifyContactName(_, unvalidatedForm)),
        FieldIds.contactPhone -> text.verifying("error.agent.addClient.contact.phone", verifyContactPhone(_, unvalidatedForm)),
        FieldIds.contactEmail -> text.verifying("error.agent.addClient.contact.email", verifyContactEmail(_, unvalidatedForm))
      ) (PreferredContact.apply)(PreferredContact.unapply)
    )
  }

  private val contactMapping = mapping(
    FieldIds.pointOfContact -> text,
    FieldIds.contactName -> text,
    FieldIds.contactPhone -> text,
    FieldIds.contactEmail -> text
  )(PreferredContact.apply)(PreferredContact.unapply)

  private def unValidatedPreferredContactForm(request: Request[_]) = Form[PreferredContact](
    contactMapping
  )

  private[addClient] def verifyContactName(name: String, preferredContact:PreferredContact) = {
    preferredContact.pointOfContact match {
      case FieldIds.me => true
      case FieldIds.other => name != null && !name.trim.isEmpty && SearchClientController.Validation.validateName(Some(name))
      case FieldIds.notUs => true
      case _ => false // unknown situation
    }
  }

  private[addClient] def verifyContactPhone(phone:String, preferredContact:PreferredContact) = {
    preferredContact.pointOfContact match {
      case FieldIds.me => true
      case FieldIds.other => validateMandatoryPhoneNumber(phone)
      case FieldIds.notUs => true
      case _ => false // unknown situation
    }
  }

  private[addClient] def verifyContactEmail(email:String, preferredContact:PreferredContact) = {
    preferredContact.pointOfContact match {
      case FieldIds.me => true
      case FieldIds.other => SearchClientController.Validation.validateEmail(Some(email))
      case FieldIds.notUs => true
      case _ => false // unknown situation
    }
  }
}
