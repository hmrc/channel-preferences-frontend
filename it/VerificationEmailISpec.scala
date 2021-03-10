/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

import org.jsoup.Jsoup
import play.api.test.Helpers._
import uk.gov.hmrc.http.SessionKeys

class VerificationEmailISpec extends EmailSupport with SessionCookieEncryptionSupport {

  "Verification email confirmation" should {
    "confirm email has been sent to the users verification email address" in {
      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()

      val (bearerToken, userId) = authHelper.authExchange(utr)

      val result =
        `/preferences/terms-and-conditions`(("Authorization", bearerToken)).postGenericOptIn(email).futureValue
      result.status must be(CREATED)
      val response =
        `/paperless/resend-verification-email`()
          .withSession(
            (SessionKeys.authToken -> bearerToken)
          )()
          .post(emptyJsonValue)
          .futureValue
      response.status must be(OK)

      val page = Jsoup.parse(response.body)
      val emailConfirmation = response.body
      emailConfirmation must include("Verification email sent")
      emailConfirmation must include(s"A new email has been sent to $email")
    }
  }

  "Attempt to verify an email" should {

    "display success message if the email link is valid" in {

      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(CREATED)

      aVerificationEmailIsReceivedFor(email)

      val response = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail()) //.futureValue

      response.futureValue.status must be(OK) //and
      response must (have(bodyWith("Email address verified")) and
        have(bodyWith("You&#x27;ve now signed up for paperless notifications.")) and
        have(bodyWith("Continue to your HMRC online account")))

      Jsoup.parse(response.futureValue.body).getElementById("link-to-home").toString() must include("/account")
    }

    "display expiry message if the link has expired" in {
      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(CREATED)

      aVerificationEmailIsReceivedFor(email)

      `/preferences-admin/sa/individual`.postExpireVerificationLink(`/entity-resolver/sa/:utr`(utr.value)).futureValue.status must be(
        OK)

      val response = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())

      response.futureValue.status must be(OK)
      response must (have(bodyWith("This link has expired")) and
        have(bodyWith("Continue to your HMRC online account")) and
        have(bodyWith("request a new verification link")))

      Jsoup.parse(response.futureValue.body).getElementById("link-to-home").toString() must include("/account")
    }

    "display already verified message if the email has been verified already" in {
      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(CREATED)

      aVerificationEmailIsReceivedFor(email)

      `/sa/print-preferences/verification`.verify(verificationTokenFromEmail()).futureValue.status must be(OK)

      val response = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())

      response.futureValue.status must be(BAD_REQUEST)
      response must (have(bodyWith("Email address already verified")) and
        have(bodyWith("Your email address has already been verified.")) and
        have(bodyWith("Continue to your HMRC online account")))

      Jsoup.parse(response.futureValue.body).getElementById("link-to-home").toString() must include("/account")
    }

    "display expired old email address message if verification link is not valid due to opt out" in {
      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(CREATED)

      withReceivedEmails(1) {
        case List(mail) =>
          mail must have('to (Some(email)), 'subject ("HMRC electronic communications: verify your email address"))
      }

      `/preferences/terms-and-conditions`(authHelper.authHeader(utr)).postGenericOptOut.futureValue.status must be(OK)

      `/sa/print-preferences/verification`.verify(verificationTokenFromEmail()) must beForAnExpiredOldEmail
    }
  }

  "Attempt to verify a change of address with an old link" should {

    "display expired old email address message if the old email is verified and new email has been verified" in {

      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      val newEmail = uniqueEmail

      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(CREATED)

      aVerificationEmailIsReceivedFor(email)

      val verificationTokenFromFirstEmail = verificationTokenFromEmail()
      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail).futureValue.status must be(OK)

      clearEmails()

      `/preferences`(authHelper.authHeader(utr)).putPendingEmail(newEmail).futureValue.status must be(OK)

      withReceivedEmails(2) { emails =>
        emails.flatMap(_.to) must contain(newEmail)
      }

      `/sa/print-preferences/verification`.verify(verificationTokenFromMultipleEmailsFor(newEmail)).futureValue.status must be(
        OK)

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) must beForAnExpiredOldEmail
    }

    "display expired old email address message if the old email is verified and the new email has not been verified" in {
      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      val newEmail = uniqueEmail
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(CREATED)

      aVerificationEmailIsReceivedFor(email)

      val verificationTokenFromFirstEmail = verificationTokenFromEmail()
      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail).futureValue.status must be(OK)

      `/preferences`(authHelper.authHeader(utr)).putPendingEmail(newEmail).futureValue.status must be(OK)

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) must beForAnExpiredOldEmail
    }

    "display expired old email address message if the old email is not verified and the new email has not been verified" in {

      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      val newEmail = uniqueEmail
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(CREATED)

      aVerificationEmailIsReceivedFor(email)

      val verificationTokenFromFirstEmail = verificationTokenFromEmail()

      `/preferences`(authHelper.authHeader(utr)).putPendingEmail(newEmail).futureValue.status must be(OK)

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) must beForAnExpiredOldEmail
    }

    "display expired old email address message if the old email is not verified and the new email is verified" in {

      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      val newEmail = uniqueEmail
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(CREATED)

      aVerificationEmailIsReceivedFor(email)

      val verificationTokenFromFirstEmail = verificationTokenFromEmail()
      clearEmails()

      `/preferences`(authHelper.authHeader(utr)).putPendingEmail(newEmail).futureValue.status must be(OK)

      aVerificationEmailIsReceivedFor(newEmail)

      `/sa/print-preferences/verification`.verify(verificationTokenFromEmail()).futureValue.status must be(OK)

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) must beForAnExpiredOldEmail

    }

    "display expired old email address message if another old email is verified and the new email is verified" in {

      val utr = Generate.utr
      val firstEmail = uniqueEmail
      val newEmail = uniqueEmail
      val secondEmail = uniqueEmail
      clearEmails()

      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(firstEmail)
        .futureValue
        .status must be(CREATED)

      aVerificationEmailIsReceivedFor(firstEmail)
      val verificationTokenFromFirstEmail = verificationTokenFromEmail()
      clearEmails()

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail).futureValue.status must be(OK)

      `/preferences`(authHelper.authHeader(utr)).putPendingEmail(secondEmail).futureValue.status must be(OK)
      clearEmails()
      `/preferences`(authHelper.authHeader(utr)).putPendingEmail(newEmail).futureValue.status must be(OK)

      withReceivedEmails(2) { emails =>
        emails.flatMap(_.to) must contain(newEmail)
      }

      `/sa/print-preferences/verification`.verify(verificationTokenFromMultipleEmailsFor(newEmail)).futureValue.status must be(
        OK)

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) must beForAnExpiredOldEmail

    }

    "display expired old email address message if another old email is verified and the new email has not been verified" in {

      val utr = Generate.utr
      clearEmails()
      val firstEmail = uniqueEmail
      val secondEmail = uniqueEmail
      val newEmail = uniqueEmail

      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(firstEmail)
        .futureValue
        .status must be(CREATED)

      aVerificationEmailIsReceivedFor(firstEmail)
      val verificationTokenFromFirstEmail = verificationTokenFromEmail()

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail).futureValue.status must be(OK)

      `/preferences`(authHelper.authHeader(utr)).putPendingEmail(secondEmail).futureValue.status must be(OK)

      `/preferences`(authHelper.authHeader(utr)).putPendingEmail(newEmail).futureValue.status must be(OK)

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) must beForAnExpiredOldEmail
    }
  }

}
