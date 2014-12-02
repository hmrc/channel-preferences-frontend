import play.api.libs.ws.WS
import play.api.Play.current

class PendingVerificationPartialISpec
  extends PreferencesFrontEndServer
  with UserAuthentication {

  "partial html for pending verification email" should {

    "contain last verification email sent date and email address" in {

      WS.url(resource("/account/preferences/warnings")).get should have(status(200))

    }
  }

}
