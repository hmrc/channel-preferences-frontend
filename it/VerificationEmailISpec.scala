import EmailSupport.Email
import org.jsoup.Jsoup
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher, Matcher}
import play.api.Play.current
import play.api.libs.json.Json
import play.api.libs.ws.{WS, WSResponse}

import scala.concurrent.Future

class VerificationEmailISpec extends PreferencesFrontEndServer  {

  "Verification email confirmation" should {
    "confirm email has been sent to the users verification email address" in new VerificationEmailTestCase with TestCaseWithFrontEndAuthentication {
      val email = uniqueEmail
      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, email) should have(status(201))

      val response = `/paperless/resend-verification-email`().withHeaders(cookie).post(emptyJsonValue)
      response should have(status(200))

      val page = Jsoup.parse(response.body)
      val emailConfirmation = response.futureValue.body
      emailConfirmation should include("Verification email sent")
      emailConfirmation should include(s"A new email has been sent to $email")
    }
  }

  "Attempt to verify an email" should {

    "display success message if the email link is valid" in new VerificationEmailTestCase {
      val email = uniqueEmail
      override val utr =
        GenerateRandom.utr().value
      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, email) should have(status(201))

      aVerificationEmailIsReceivedFor(email)

      val response = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())

      response should (have(status(200)) and
        have(bodyWith("Email address verified")) and
        have(bodyWith("You&#x27;ve now signed up for paperless notifications.")) and
        have(bodyWith("Sign into your HMRC online account")) and
        have(bodyWith( """href="https://online.hmrc.gov.uk"""")))
    }

    "display expiry message if the link has expired" in new VerificationEmailTestCase {
      val email = uniqueEmail
      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, email) should have(status(201))

      aVerificationEmailIsReceivedFor(email)

      `/preferences-admin/sa/individual`.postExpireVerificationLink(utr) should have(status(200))

      val response = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())

      response should (have(status(200)) and
        have(bodyWith("This link has expired")) and
        have(bodyWith("Sign into your HMRC online account")) and
        have(bodyWith( """href="https://online.hmrc.gov.uk"""")) and
        have(bodyWith("request a new verification link")))
    }

    "display already verified message if the email has been verified already" in new VerificationEmailTestCase {
      val email = uniqueEmail
      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, email) should have(status(201))

      aVerificationEmailIsReceivedFor(email)

      `/sa/print-preferences/verification`.verify(verificationTokenFromEmail()) should have(status(200))

      val response = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())

      response should (have(status(400)) and
        have(bodyWith("Email address already verified")) and
        have(bodyWith("Your email address has already been verified.")) and
        have(bodyWith("Sign into your HMRC online account")) and
        have(bodyWith( """href="https://online.hmrc.gov.uk"""")))
    }

    "display expired old email address message if verification link is not valid due to opt out" in new VerificationEmailTestCase {
      val email = uniqueEmail
      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, email) should have(status(201))

      withReceivedEmails(1) { case List(mail) =>
        mail should have(
          'to(Some(email)),
          'subject("HMRC paperless notifications: verify your email address"))
      }

      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postOptOut(utr) should have(status(200))

      `/sa/print-preferences/verification`.verify(verificationTokenFromEmail()) should beForAnExpiredOldEmail
    }
  }

  "Attempt to verify a change of address with an old link" should {

    "display expired old email address message if the old email is verified and new email has been verified" in new VerificationEmailTestCase {
      val email = uniqueEmail
      val newEmail = uniqueEmail

      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, email) should have(status(201))

      aVerificationEmailIsReceivedFor(email)

      val verificationTokenFromFirstEmail = verificationTokenFromEmail()
      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should have(status(200))

      clearEmails()

      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, newEmail) should have(status(200))
      withReceivedEmails(2) { emails =>
        emails.flatMap(_.to) should contain(newEmail)
      }

      `/sa/print-preferences/verification`.verify(verificationTokenFromMultipleEmailsFor(newEmail)) should have(status(200))

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should beForAnExpiredOldEmail
    }

    "display expired old email address message if the old email is verified and the new email has not been verified" in new VerificationEmailTestCase {
      val email = uniqueEmail
      val newEmail = uniqueEmail
      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, email) should have(status(201))

      aVerificationEmailIsReceivedFor(email)

      val verificationTokenFromFirstEmail = verificationTokenFromEmail()
      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should have(status(200))

      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, newEmail) should have(status(200))

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should beForAnExpiredOldEmail
    }

    "display expired old email address message if the old email is not verified and the new email has not been verified" in new VerificationEmailTestCase {
      val email = uniqueEmail
      val newEmail = uniqueEmail
      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, email) should have(status(201))

      aVerificationEmailIsReceivedFor(email)

      val verificationTokenFromFirstEmail = verificationTokenFromEmail()

      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, newEmail) should have(status(200))

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should beForAnExpiredOldEmail
    }

    "display expired old email address message if the old email is not verified and the new email is verified" in new VerificationEmailTestCase {
      val email = uniqueEmail
      val newEmail = uniqueEmail
      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, email) should have(status(201))

      aVerificationEmailIsReceivedFor(email)

      val verificationTokenFromFirstEmail = verificationTokenFromEmail()
      clearEmails()

      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, newEmail) should have(status(200))

      aVerificationEmailIsReceivedFor(newEmail)

      `/sa/print-preferences/verification`.verify(verificationTokenFromEmail()) should have(status(200))

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should beForAnExpiredOldEmail

    }

    "display expired old email address message if another old email is verified and the new email is verified" in new VerificationEmailTestCase {
      val firstEmail = uniqueEmail
      val newEmail = uniqueEmail
      val secondEmail = uniqueEmail

      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, firstEmail) should have(status(201))

      aVerificationEmailIsReceivedFor(firstEmail)
      val verificationTokenFromFirstEmail = verificationTokenFromEmail()
      clearEmails()

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should have(status(200))

      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, secondEmail) should have(status(200))
      clearEmails()
      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, newEmail) should have(status(200))

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

      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, firstEmail) should have(status(201))

      aVerificationEmailIsReceivedFor(firstEmail)
      val verificationTokenFromFirstEmail = verificationTokenFromEmail()

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should have(status(200))

      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, secondEmail) should have(status(200))

      `/preferences/sa/individual/utr/terms-and-conditions`(ggAuthHeader).postPendingEmail(utr, newEmail) should have(status(200))

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) should beForAnExpiredOldEmail
    }
  }

  trait VerificationEmailTestCase extends TestCaseWithFrontEndAuthentication with EmailSupport with Eventually {
    clearEmails()
    `/preferences-admin/sa/individual`.delete(utr) should have(status(200))

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
          'to(Some(email)),
          'subject("HMRC paperless notifications: verify your email address")
        )
      }
    }

    def aVerificationEmailIsReceivedForNewEmail(email: String) {
      withReceivedEmails(2) { case List(mail) =>
        mail should have(
          'to(Some(email)),
          'subject("Self Assessment reminders: verify your new email address")
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
