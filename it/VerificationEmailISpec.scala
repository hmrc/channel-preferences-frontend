import java.util.UUID

import EmailSupport.Email
import play.api.Play.current
import play.api.libs.json.Json
import play.api.libs.ws.{WSResponse, WS}
import org.scalatest.concurrent.Eventually
import play.api.mvc.Results.EmptyContent

import scala.concurrent.{Future, Await}

class VerificationEmailISpec extends PreferencesFrontEndServer with UserAuthentication {

  "Verification email confirmation" should {
    "confirm email has been sent to the users verification email address" in new VerificationEmailTestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))

      val response = `/resend-validation-email`.withHeaders(authenticationCookie(userId, password)).post(emptyJsonValue)
      response should have(status(200))
      response.futureValue.body should include(s"A new email has been sent to $email")
    }
  }

  "Attempt to verify an email" should {

    "display success message if the email link is valid" in new VerificationEmailTestCase {

      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))

      withReceivedEmails(1) { case List(mail) =>
        mail should have(
          'to(Some(email)),
          'subject("Self Assessment reminders: verify your email address")
        )

        val response = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())
        response should have(status(200))
        response.futureValue.body should (
            include ("Email address verified") and
            include ("You’re now signed up for Self Assessment email reminders.") and
            include ("Most of your reminders will now be sent by email.") and
            include("Sign into your HMRC online account") and
            include("""href="https://online.hmrc.gov.uk"""")
          )
      }
    }

    "display failure message if the link has expired" in new VerificationEmailTestCase {

      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))

      withReceivedEmails(1) { case List(mail) =>
        mail should have(
          'to(Some(email)),
          'subject("Self Assessment reminders: verify your email address")
        )

        `/preferences-admin/sa/individual`.postExpireVerificationLink(utr) should have(status(200))

        val response = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())
        response should have (status(200))
        response.futureValue.body should (
          include("This link has expired") and
          include("Sign into your HMRC online account") and
          include("""href="https://online.hmrc.gov.uk"""") and
          include("go to 'Your details' to request a new verification link")
        )
      }
    }

    "display failure message if the email has been verified already" in new VerificationEmailTestCase {
      val email = uniqueEmail
      `/portal/preferences/sa/individual`.postPendingEmail(utr, email) should have(status(201))

      withReceivedEmails(1) { case List(mail) =>
        mail should have(
          'to(Some(email)),
          'subject("Self Assessment reminders: verify your email address")
        )

        `/sa/print-preferences/verification`.verify(verificationTokenFromEmail()) should have (status(200))

        val response = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())
        response should have(status(400))
        response.futureValue.body should (
          include ("Email address already verified") and
          include ("Your email address has already been verified.") and
          include("Sign into your HMRC online account") and
          include("""href="https://online.hmrc.gov.uk"""")
        )
      }
    }
  }


trait VerificationEmailTestCase extends TestCase with EmailSupport with Eventually {
    clearEmails()

    `/preferences-admin/sa/individual`.delete(utr) should have(status(200))

    val emptyJsonValue = Json.parse("{}")

    def `/resend-validation-email` = WS.url(resource("/account/account-details/sa/resend-validation-email"))

    val `/sa/print-preferences/verification` = new {
      def verify(token: String) = WS.url(resource(s"/sa/print-preferences/verification/$token")).get()
    }

    def withReceivedEmails(expectedCount: Int)(assertions: List[Email] => Unit) {
      val listOfMails = eventually {
        val emailList = await(emails)
        emailList should have size expectedCount
        emailList
      }

      assertions(listOfMails)
    }

  }

}

