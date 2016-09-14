import EmailSupport.Email
import org.jsoup.Jsoup
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher, Matcher}
import play.api.Play.current
import play.api.libs.json.Json
import play.api.libs.ws.{WS, WSResponse}

import scala.concurrent.Future

class VerificationEmailISpec extends PreferencesFrontEndServer {

  "Verification email confirmation" should {
    "confirm email has been sent to the users verification email address" in new VerificationEmailTestCase with TestCaseWithFrontEndAuthentication {
      val email = uniqueEmail

      val result = `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postPendingEmail(email).futureValue
      result.status should be(201)

      val response = `/paperless/resend-verification-email`().withHeaders(cookieWithUtr).post(emptyJsonValue).futureValue
      response.status should be(200)

      val page = Jsoup.parse(response.body)
      val emailConfirmation = response.body
      emailConfirmation should include("Verification email sent")
      emailConfirmation should include(s"A new email has been sent to $email")
    }
  }

  "Attempt to verify an email" should {

    "display success message if the email link is valid" in new VerificationEmailTestCase {
      val email = uniqueEmail
      override val utr =
        GenerateRandom.utr().value
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postPendingEmail(email) should have(status(201))

      aVerificationEmailIsReceivedFor(email)

      val response = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())

      response should (have(status(200)) and
        have(bodyWith("Email address verified")) and
        have(bodyWith("You&#x27;ve now signed up for paperless notifications.")) and
        have(bodyWith("Continue to your HMRC online account")))

      Jsoup.parse(response.body).getElementById("link-to-home").toString() should include ("/account")
    }

    "display expiry message if the link has expired" in new VerificationEmailTestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postPendingEmail(email) should have(status(201))

      aVerificationEmailIsReceivedFor(email)

      `/preferences-admin/sa/individual`.postExpireVerificationLink(`/entity-resolver-admin/sa/:utr`(utr)) should have(status(200))

      val response = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())

      response should (have(status(200)) and
        have(bodyWith("This link has expired")) and
        have(bodyWith("Continue to your HMRC online account")) and
        have(bodyWith("request a new verification link")))

      Jsoup.parse(response.body).getElementById("link-to-home").toString() should include ("/account")
    }

    "display already verified message if the email has been verified already" in new VerificationEmailTestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postPendingEmail(email) should have(status(201))

      aVerificationEmailIsReceivedFor(email)

      `/sa/print-preferences/verification`.verify(verificationTokenFromEmail()) should have(status(200))

      val response = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())

      response should (have(status(400)) and
        have(bodyWith("Email address already verified")) and
        have(bodyWith("Your email address has already been verified.")) and
        have(bodyWith("Continue to your HMRC online account")))

      Jsoup.parse(response.body).getElementById("link-to-home").toString() should include ("/account")
    }

    "display expired old email address message if verification link is not valid due to opt out" in new VerificationEmailTestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postPendingEmail(email) should have(status(201))

      withReceivedEmails(1) { case List(mail) =>
        mail should have(
          'to (Some(email)),
          'subject ("HMRC paperless notifications: verify your email address"))
      }

      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postOptOut should have(status(200))

      `/sa/print-preferences/verification`.verify(verificationTokenFromEmail()) should beForAnExpiredOldEmail
    }
  }

  "Attempt to verify a change of address with an old link" should {

    "display expired old email address message if the old email is verified and new email has been verified" in new VerificationEmailTestCase {
      val email = uniqueEmail
      val newEmail = uniqueEmail

      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postPendingEmail(email) should have(status(201))

      aVerificationEmailIsReceivedFor(email)

      val verificationTokenFromFirstEmail = verificationTokenFromEmail()
      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should have(status(200))

      clearEmails()

      `/preferences`(ggAuthHeaderWithUtr).postPendingEmail(newEmail) should have(status(200))

      withReceivedEmails(2) { emails =>
        emails.flatMap(_.to) should contain(newEmail)
      }

      `/sa/print-preferences/verification`.verify(verificationTokenFromMultipleEmailsFor(newEmail)) should have(status(200))

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should beForAnExpiredOldEmail
    }

    "display expired old email address message if the old email is verified and the new email has not been verified" in new VerificationEmailTestCase {
      val email = uniqueEmail
      val newEmail = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postPendingEmail(email) should have(status(201))

      aVerificationEmailIsReceivedFor(email)

      val verificationTokenFromFirstEmail = verificationTokenFromEmail()
      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should have(status(200))

      `/preferences`(ggAuthHeaderWithUtr).postPendingEmail(newEmail) should have(status(200))

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should beForAnExpiredOldEmail
    }

    "display expired old email address message if the old email is not verified and the new email has not been verified" in new VerificationEmailTestCase {
      val email = uniqueEmail
      val newEmail = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postPendingEmail(email) should have(status(201))

      aVerificationEmailIsReceivedFor(email)

      val verificationTokenFromFirstEmail = verificationTokenFromEmail()

      `/preferences`(ggAuthHeaderWithUtr).postPendingEmail(newEmail) should have(status(200))

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should beForAnExpiredOldEmail
    }

    "display expired old email address message if the old email is not verified and the new email is verified" in new VerificationEmailTestCase {
      val email = uniqueEmail
      val newEmail = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postPendingEmail(email) should have(status(201))

      aVerificationEmailIsReceivedFor(email)

      val verificationTokenFromFirstEmail = verificationTokenFromEmail()
      clearEmails()

      `/preferences`(ggAuthHeaderWithUtr).postPendingEmail(newEmail) should have(status(200))

      aVerificationEmailIsReceivedFor(newEmail)

      `/sa/print-preferences/verification`.verify(verificationTokenFromEmail()) should have(status(200))

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should beForAnExpiredOldEmail

    }

    "display expired old email address message if another old email is verified and the new email is verified" in new VerificationEmailTestCase {
      val firstEmail = uniqueEmail
      val newEmail = uniqueEmail
      val secondEmail = uniqueEmail

      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postPendingEmail(firstEmail) should have(status(201))

      aVerificationEmailIsReceivedFor(firstEmail)
      val verificationTokenFromFirstEmail = verificationTokenFromEmail()
      clearEmails()

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should have(status(200))

      `/preferences`(ggAuthHeaderWithUtr).postPendingEmail(secondEmail) should have(status(200))
      clearEmails()
      `/preferences`(ggAuthHeaderWithUtr).postPendingEmail(newEmail) should have(status(200))

      withReceivedEmails(2) { emails =>
        emails.flatMap(_.to) should contain(newEmail)
      }

      `/sa/print-preferences/verification`.verify(verificationTokenFromMultipleEmailsFor(newEmail)) should have(status(200))

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should beForAnExpiredOldEmail

    }

    "display expired old email address message if another old email is verified and the new email has not been verified" in new VerificationEmailTestCase {
      val firstEmail = uniqueEmail
      val secondEmail = uniqueEmail
      val newEmail = uniqueEmail

      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postPendingEmail(firstEmail) should have(status(201))

      aVerificationEmailIsReceivedFor(firstEmail)
      val verificationTokenFromFirstEmail = verificationTokenFromEmail()

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should have(status(200))

      `/preferences`(ggAuthHeaderWithUtr).postPendingEmail(secondEmail) should have(status(200))

      `/preferences`(ggAuthHeaderWithUtr).postPendingEmail(newEmail) should have(status(200))

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should beForAnExpiredOldEmail
    }
  }

  trait VerificationEmailTestCase extends TestCaseWithFrontEndAuthentication with EmailSupport with Eventually {
    clearEmails()

    val emptyJsonValue = Json.parse("{}")

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

    def aVerificationEmailIsReceivedFor(email: String) {
      withReceivedEmails(1) { case List(mail) =>
        mail should have(
          'to (Some(email)),
          'subject ("HMRC paperless notifications: verify your email address")
        )
      }
    }

    def aVerificationEmailIsReceivedForNewEmail(email: String) {
      withReceivedEmails(2) { case List(mail) =>
        mail should have(
          'to (Some(email)),
          'subject ("Self Assessment reminders: verify your new email address")
        )
      }
    }

    def beForAnExpiredOldEmail: Matcher[Future[WSResponse]] = {
      have(status(200)) and
        have(bodyWith("You&#x27;ve used a link that has now expired")) and
        have(bodyWith("It may have been sent to an old or alternative email address.")) and
        have(bodyWith("Please use the link in the latest verification email sent to your specified email address."))
    }
  }

  def bodyWith(expected: String) = new HavePropertyMatcher[Future[WSResponse], String] {
    def apply(response: Future[WSResponse]) = HavePropertyMatchResult(
      matches = response.futureValue.body.contains(expected),
      propertyName = "Response Body",
      expectedValue = expected,
      actualValue = response.futureValue.body
    )
  }
}
