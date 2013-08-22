package controllers.agent.registration

import play.api.mvc.Session
import controllers.common.service.MicroServices

trait MultiformRegistration extends MicroServices {

  val phoneNumberErrorKey = "error.agent.phone"

  def saveFormToKeyStore(formName: String, formData: Map[String, Any])(implicit session: Session) {
    keyStoreMicroService.addKeyStoreEntry("Registration:" + session.get("PLAY_SESSION"), "agent", formName, formData)
  }

  def validateMandatoryPhoneNumber = { s: String => s.matches("\\d+") }
  def validateOptionalPhoneNumber = { s: String => s.matches("\\d*") }
  def validateOptionalEmail = { s: String => s.isEmpty || s.matches("""\b[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*\b""") }
  def validateSaUtr = { s: String => s.matches("\\d(10)") }

}
