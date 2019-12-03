/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import EmailSupport.Email
import org.jsoup.Jsoup
import org.scalatest.matchers.{ HavePropertyMatchResult, HavePropertyMatcher, Matcher }
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

class VerificationEmailISpec extends EmailSupport {

  "Verification email confirmation" should {
    "confirm email has been sent to the users verification email address" in new VerificationEmailTestCase {
      val email = uniqueEmail

      val result = `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue
      result.status must be(201)

      val response =
        `/paperless/resend-verification-email`().withHttpHeaders(cookieWithUtr).post(emptyJsonValue).futureValue
      response.status must be(200)

      val page = Jsoup.parse(response.body)
      val emailConfirmation = response.body
      emailConfirmation must include("Verification email sent")
      emailConfirmation must include(s"A new email has been sent to $email")
    }
  }

  "Attempt to verify an email" should {

    "display success message if the email link is valid" in new VerificationEmailTestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)

      aVerificationEmailIsReceivedFor(email)

      val response = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail()) //.futureValue

      response.futureValue.status must be(200) //and
      response must (have(bodyWith("Email address verified")) and
        have(bodyWith("You&#x27;ve now signed up for paperless notifications.")) and
        have(bodyWith("Continue to your HMRC online account")))

      Jsoup.parse(response.futureValue.body).getElementById("link-to-home").toString() must include("/account")
    }

    "display expiry message if the link has expired" in new VerificationEmailTestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)

      aVerificationEmailIsReceivedFor(email)

      `/preferences-admin/sa/individual`.postExpireVerificationLink(`/entity-resolver/sa/:utr`(utr.value)).futureValue.status must be(
        200)

      val response = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())

      response.futureValue.status must be(200)
      response must (have(bodyWith("This link has expired")) and
        have(bodyWith("Continue to your HMRC online account")) and
        have(bodyWith("request a new verification link")))

      Jsoup.parse(response.futureValue.body).getElementById("link-to-home").toString() must include("/account")
    }

    "display already verified message if the email has been verified already" in new VerificationEmailTestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)

      aVerificationEmailIsReceivedFor(email)

      `/sa/print-preferences/verification`.verify(verificationTokenFromEmail()).futureValue.status must be(200)

      val response = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())

      response.futureValue.status must be(400)
      response must (have(bodyWith("Email address already verified")) and
        have(bodyWith("Your email address has already been verified.")) and
        have(bodyWith("Continue to your HMRC online account")))

      Jsoup.parse(response.futureValue.body).getElementById("link-to-home").toString() must include("/account")
    }

    "display expired old email address message if verification link is not valid due to opt out" in new VerificationEmailTestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)

      withReceivedEmails(1) {
        case List(mail) =>
          mail must have('to (Some(email)), 'subject ("HMRC electronic communications: verify your email address"))
      }

      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptOut.futureValue.status must be(200)

      `/sa/print-preferences/verification`.verify(verificationTokenFromEmail()) must beForAnExpiredOldEmail
    }
  }

  "Attempt to verify a change of address with an old link" should {

    "display expired old email address message if the old email is verified and new email has been verified" in new VerificationEmailTestCase {
      val email = uniqueEmail
      val newEmail = uniqueEmail

      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)

      aVerificationEmailIsReceivedFor(email)

      val verificationTokenFromFirstEmail = verificationTokenFromEmail()
      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail).futureValue.status must be(200)

      clearEmails()

      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(newEmail).futureValue.status must be(200)

      withReceivedEmails(2) { emails =>
        emails.flatMap(_.to) must contain(newEmail)
      }

      `/sa/print-preferences/verification`.verify(verificationTokenFromMultipleEmailsFor(newEmail)).futureValue.status must be(
        200)

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) must beForAnExpiredOldEmail
    }

    "display expired old email address message if the old email is verified and the new email has not been verified" in new VerificationEmailTestCase {
      val email = uniqueEmail
      val newEmail = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)

      aVerificationEmailIsReceivedFor(email)

      val verificationTokenFromFirstEmail = verificationTokenFromEmail()
      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail).futureValue.status must be(200)

      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(newEmail).futureValue.status must be(200)

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) must beForAnExpiredOldEmail
    }

    "display expired old email address message if the old email is not verified and the new email has not been verified" in new VerificationEmailTestCase {
      val email = uniqueEmail
      val newEmail = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)

      aVerificationEmailIsReceivedFor(email)

      val verificationTokenFromFirstEmail = verificationTokenFromEmail()

      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(newEmail).futureValue.status must be(200)

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) must beForAnExpiredOldEmail
    }

    "display expired old email address message if the old email is not verified and the new email is verified" in new VerificationEmailTestCase {
      val email = uniqueEmail
      val newEmail = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)

      aVerificationEmailIsReceivedFor(email)

      val verificationTokenFromFirstEmail = verificationTokenFromEmail()
      clearEmails()

      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(newEmail).futureValue.status must be(200)

      aVerificationEmailIsReceivedFor(newEmail)

      `/sa/print-preferences/verification`.verify(verificationTokenFromEmail()).futureValue.status must be(200)

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) must beForAnExpiredOldEmail

    }

    "display expired old email address message if another old email is verified and the new email is verified" in new VerificationEmailTestCase {
      val firstEmail = uniqueEmail
      val newEmail = uniqueEmail
      val secondEmail = uniqueEmail

      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(firstEmail).futureValue.status must be(
        201)

      aVerificationEmailIsReceivedFor(firstEmail)
      val verificationTokenFromFirstEmail = verificationTokenFromEmail()
      clearEmails()

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail).futureValue.status must be(200)

      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(secondEmail).futureValue.status must be(200)
      clearEmails()
      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(newEmail).futureValue.status must be(200)

      withReceivedEmails(2) { emails =>
        emails.flatMap(_.to) must contain(newEmail)
      }

      `/sa/print-preferences/verification`.verify(verificationTokenFromMultipleEmailsFor(newEmail)).futureValue.status must be(
        200)

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) must beForAnExpiredOldEmail

    }

    "display expired old email address message if another old email is verified and the new email has not been verified" in new VerificationEmailTestCase {
      val firstEmail = uniqueEmail
      val secondEmail = uniqueEmail
      val newEmail = uniqueEmail

      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(firstEmail).futureValue.status must be(
        201)

      aVerificationEmailIsReceivedFor(firstEmail)
      val verificationTokenFromFirstEmail = verificationTokenFromEmail()

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail).futureValue.status must be(200)

      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(secondEmail).futureValue.status must be(200)

      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(newEmail).futureValue.status must be(200)

      `/sa/print-preferences/verification`.verify(verificationTokenFromFirstEmail) must beForAnExpiredOldEmail
    }
  }

  trait VerificationEmailTestCase {
//      override protected def mode: Mode = Mode.Test
//
//      override protected def runModeConfiguration: Configuration = runModeConfiguration
    val utr = Generate.utr
    val nino = Generate.nino
    val ggAuthHeaderWithUtr = authHelper.authHeader(utr)
    val ggAuthHeaderWithNino = authHelper.authHeader(nino)
    val cookieWithUtr = ggAuthHeaderWithUtr
    val cookieWithNino = ggAuthHeaderWithNino
    clearEmails()

    val emptyJsonValue = Json.parse("{}")

    val `/sa/print-preferences/verification` = new {
      def verify(token: String) = wsUrl(s"/sa/print-preferences/verification/$token").get()
    }

    def withReceivedEmails(expectedCount: Int)(assertions: List[Email] => Unit) {
      val listOfMails = eventually {
        val emailList = emails.futureValue
        emailList must have size expectedCount
        emailList
      }
      assertions(listOfMails)
    }

    def aVerificationEmailIsReceivedFor(email: String) {
      withReceivedEmails(1) {
        case List(mail) =>
          mail must have(
            'to (Some(email)),
            'subject ("HMRC electronic communications: verify your email address")
          )
      }
    }

    def beForAnExpiredOldEmail: Matcher[Future[WSResponse]] =
      have(statusWith(200)) and
        have(bodyWith("You&#x27;ve used a link that has now expired")) and
        have(bodyWith("It may have been sent to an old or alternative email address.")) and
        have(bodyWith("Please use the link in the latest verification email sent to your specified email address."))
  }

  def bodyWith(expected: String) = new HavePropertyMatcher[Future[WSResponse], String] {
    def apply(response: Future[WSResponse]) = HavePropertyMatchResult(
      matches = response.futureValue.body.contains(expected),
      propertyName = "Response Body",
      expectedValue = expected,
      actualValue = response.futureValue.body
    )
  }
  def statusWith(expected: Int) = new HavePropertyMatcher[Future[WSResponse], Int] {
    def apply(response: Future[WSResponse]) = HavePropertyMatchResult(
      matches = response.futureValue.status.equals(expected),
      propertyName = "Response Status",
      expectedValue = expected,
      actualValue = response.futureValue.status
    )
  }
}
