package controllers.agent.registration

import play.api.mvc.Session
import controllers.common.service.MicroServices

trait MultiformRegistration extends MicroServices {
  def saveFormToKeyStore(formName: String, formData: Map[String, Any])(implicit session: Session) {
    keyStoreMicroService.addKeyStoreEntry("Registration:" + session.get("PLAY_SESSION"), "agent", formName, formData)
  }
}
