package controllers.agent.addClient

import controllers.common.{ActionWrappers, BaseController}
import controllers.common.validators.Validators
import play.api.mvc.{SimpleResult, Request}
import views.html.agents.addClient.{client_successfully_added, preferred_contact}
import SearchClientController.KeyStoreKeys._
import play.api.data.Form
import play.api.data.Forms._
import controllers.agent.addClient.PreferredClientController._
import uk.gov.hmrc.common.microservice.domain.User
import models.agent.addClient.{PotentialClient, PreferredContactData}
import scala.Some
import Validators.validateMandatoryPhoneNumber
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import models.agent.{Client, Contact, PreferredContact}
import service.agent.AgentMicroServices

class PreferredContactController
  extends BaseController
  with ActionWrappers with AgentMicroServices {

  def preferredContact = AuthorisedForIdaAction(Some(PayeRegime)) {
    preferredContactAction
  }

  private[agent] def preferredContactAction(user: User)(request: Request[_]): SimpleResult = {
    val form = preferredContactForm(request).bindFromRequest()(request)
    val ksId = keystoreId(user.oid, form(FieldIds.instanceId).value.getOrElse("instanceIdNotFound"))
    keyStoreMicroService.getEntry[PotentialClient](ksId, serviceSourceKey, addClientKey) match {
      case Some(pc@PotentialClient(Some(_), Some(_), _)) => {
        //FIXME we should trim contact details before saving them here
        form.fold(
          errors => BadRequest(preferred_contact(form)),
          search => {
            val addClientUri = user.regimes.agent.get.actions.get("addClient").getOrElse(throw new IllegalArgumentException("No addClient action uri found"))
            val prefContact = search._1

            val contact: PreferredContact = prefContact.pointOfContact match {
              case FieldIds.me => PreferredContact(true, None)
              case FieldIds.other => PreferredContact(true,
                Some(Contact(prefContact.contactName, prefContact.contactEmail, prefContact.contactPhone)))
              case _ => PreferredContact(false, None)
            }

            val client: Client = Client(pc.clientSearch.get.nino, pc.confirmation.get.internalClientReference, contact)
            agentMicroService.saveOrUpdateClient(addClientUri, client)
            keyStoreMicroService.deleteKeyStore(ksId, serviceSourceKey)
            Ok(client_successfully_added(client))
          }
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
    private[addClient] val instanceId = "instanceId"

    val me = "me"
    private[addClient] val other = "other"
    private[addClient] val notUs = "notUs"
  }

  private[addClient] def emptyUnValidatedPreferredContactForm() = Form[(PreferredContactData, String)](
    mapping(
      FieldIds.pointOfContact -> text,
      FieldIds.contactName -> text,
      FieldIds.contactPhone -> text,
      FieldIds.contactEmail -> text,
      FieldIds.instanceId -> nonEmptyText
    )((pointOfContact, contactName, contactPhone, contactEmail, instanceId) => (PreferredContactData(pointOfContact, contactName, contactPhone, contactEmail), instanceId))
      (preferredContactWithInstanceId => Some((preferredContactWithInstanceId._1.pointOfContact, preferredContactWithInstanceId._1.contactName, preferredContactWithInstanceId._1.contactPhone, preferredContactWithInstanceId._1.contactEmail, preferredContactWithInstanceId._2)))
  )

  private[addClient] def preferredContactForm(request: Request[_]) = {
    lazy val unvalidatedForm = unValidatedPreferredContactForm(request).bindFromRequest()(request).get
    Form[(PreferredContactData, String)](
      mapping(
        FieldIds.pointOfContact -> text,
        FieldIds.contactName -> text.verifying("error.agent.addClient.contact.name", verifyContactName(_, unvalidatedForm)),
        FieldIds.contactPhone -> text.verifying("error.agent.addClient.contact.phone", verifyContactPhone(_, unvalidatedForm)),
        FieldIds.contactEmail -> text.verifying("error.agent.addClient.contact.email", verifyContactEmail(_, unvalidatedForm)),
        FieldIds.instanceId -> nonEmptyText
      )((pointOfContact, contactName, contactPhone, contactEmail, instanceId) => (PreferredContactData(pointOfContact, contactName, contactPhone, contactEmail), instanceId))
        (preferredContactWithInstanceId => Some((preferredContactWithInstanceId._1.pointOfContact, preferredContactWithInstanceId._1.contactName, preferredContactWithInstanceId._1.contactPhone, preferredContactWithInstanceId._1.contactEmail, preferredContactWithInstanceId._2)))
    )
  }

  private def unValidatedPreferredContactForm(request: Request[_]) = Form[PreferredContactData](
    mapping(
      FieldIds.pointOfContact -> text,
      FieldIds.contactName -> text,
      FieldIds.contactPhone -> text,
      FieldIds.contactEmail -> text
    )(PreferredContactData.apply)(PreferredContactData.unapply)
  )

  private[addClient] def verifyContactName(name: String, preferredContact: PreferredContactData) = {
    preferredContact.pointOfContact match {
      case FieldIds.me => true
      case FieldIds.other => name != null && !name.trim.isEmpty && SearchClientController.Validation.validateName(Some(name))
      case FieldIds.notUs => true
      case _ => false // unknown situation
    }
  }

  private[addClient] def verifyContactPhone(phone: String, preferredContact: PreferredContactData) = {
    preferredContact.pointOfContact match {
      case FieldIds.me => true
      case FieldIds.other => validateMandatoryPhoneNumber(phone)
      case FieldIds.notUs => true
      case _ => false // unknown situation
    }
  }

  private[addClient] def verifyContactEmail(email: String, preferredContact: PreferredContactData) = {
    preferredContact.pointOfContact match {
      case FieldIds.me => true
      case FieldIds.other => SearchClientController.Validation.validateEmail(Some(email))
      case FieldIds.notUs => true
      case _ => false // unknown situation
    }
  }
}
