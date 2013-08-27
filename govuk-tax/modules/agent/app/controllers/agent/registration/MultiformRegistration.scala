package controllers.agent.registration

import play.api.mvc.{ AnyContent, Request, Session }
import controllers.common.service.MicroServices
import uk.gov.hmrc.microservice.domain.User

trait MultiformRegistration extends MicroServices {

  val phoneNumberErrorKey = "error.agent.phone"

  def saveFormToKeyStore(formName: String, formData: Map[String, Any], userId: String) {
    keyStoreMicroService.addKeyStoreEntry("Registration:" + userId, "agent", formName, formData)
  }

  def validateMandatoryPhoneNumber = { s: String => s.matches("\\d+") }
  def validateOptionalPhoneNumber = { s: String => s.matches("\\d*") }
  def validateOptionalEmail = { s: String => s.isEmpty || s.matches("""\b[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*\b""") }
  def validateSaUtr = { s: String => s.matches("\\d{10}") }
  def userId(user: User) = { user.user.substring(user.user.lastIndexOf("/") + 1) }
}
