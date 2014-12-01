import java.util.UUID

import play.api.Play.current
import play.api.libs.json.Json
import play.api.libs.ws.WS

class VerificationEmailResentConfirmationISpec extends PreferencesFrontEndServer with UserAuthentication {

  "Verification email confirmation" should {
    "confirm email has been sent to the users verification email address" in new TestCase {
      val email = s"${UUID.randomUUID().toString}@email.com"
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))

      val response = `/resend-validation-email`.withHeaders(authenticationCookie(userId, password)).post(emptyJsonValue)
      response should have(status(200))
      response.futureValue.body should include(s"A new email has been sent to $email")
    }
  }

  trait TestCase {
    val emptyJsonValue = Json.parse("{}")

    def `/resend-validation-email` = WS.url(resource("/account/account-details/sa/resend-validation-email"))

    val `/portal/preferences/sa/individual` = new {
      def postPendingEmail(utr: String, pendingEmail: String) = WS.url(server.externalResource("preferences",
        s"/portal/preferences/sa/individual/$utr/print-suppression")).post(Json.parse( s"""{"digital": true, "email":"$pendingEmail"}"""))
    }
  }
}
