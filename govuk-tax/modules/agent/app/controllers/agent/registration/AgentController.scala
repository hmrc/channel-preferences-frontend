package controllers.agent.registration

import uk.gov.hmrc.microservice.domain.User
import controllers.common.service.MicroServices
import uk.gov.hmrc.common.microservice.keystore.KeyStore
import controllers.common.{ ActionWrappers, SessionTimeoutWrapper, BaseController }

/**
 * @author Eric Vlaanderen
 */
trait AgentController extends MicroServices with BaseController with SessionTimeoutWrapper with ActionWrappers {

  def userId(user: User) = { user.user.substring(user.user.lastIndexOf("/") + 1) }

  def saveFormToKeyStore(formName: String, formData: Map[String, Any], userId: String) {
    keyStoreMicroService.addKeyStoreEntry("Registration:" + userId, "agent", formName, formData)
  }

  def getKeyStore(userId: String): Option[KeyStore] = {
    keyStoreMicroService.getKeyStore("Registration:" + userId, "agent")
  }

  def deleteFromKeyStore(userId: String) = {
    keyStoreMicroService.deleteKeyStore("Registration:" + userId, "agent")
  }

}

object FormNames {
  val professionalBodyMembershipFormName = "professionalBodyMembershipForm"
  val companyDetailsFormName = "companyDetailsForm"
  val agentTypeAndLegalEntityFormName = "agentTypeAndLegalEntityForm"
  val contactFormName = "contactForm"
}
