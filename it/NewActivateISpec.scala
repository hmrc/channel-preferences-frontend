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

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._

class NewActivateGraceOutISpec extends EmailSupport with SessionCookieEncryptionSupport {
  private def additionalConfig =
    Map(
      "controllers.controllers.internal.ActivationController.needsAuth" -> false,
      "Test.activation.gracePeriodInMin"                                -> 0
    )

  override lazy val app = new GuiceApplicationBuilder()
    .configure(additionalConfig)
    .build()

  "activate with grace period already passed" should {

    "return PRECONDITION_FAILED for existing PTA Customer who had previously opted out and has no email held in preferences" in {

      val utr = Generate.utr
      clearEmails()
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr)).postGenericOptOut.futureValue.status must be(201)

      val response = `/paperless/activate`(utr)().put().futureValue
      response.status must be(PRECONDITION_FAILED)

    }

    "return OK for  existing Opted-in customer with  unverified email" in {

      val utr = Generate.utr
      clearEmails()
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(201)

      val response = `/paperless/activate`(utr)().put().futureValue
      response.status must be(OK)
    }

    "return OK for Existing Opted-in customer with verified email)" in {
      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(201)
      aVerificationEmailIsReceivedFor(email)

      val verificationResponse = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())
      verificationResponse.futureValue.status must be(200)

      verificationResponse must (have(bodyWith("Email address verified")) and
        have(bodyWith("You&#x27;ve now signed up for paperless notifications.")) and
        have(bodyWith("Continue to your HMRC online account")))

      val response = `/paperless/activate`(utr)().put().futureValue
      response.status must be(OK)
    }

    "return PRECONDITION_FAILED for Existing  Opted-out customer who was previously Opted-in with verified email)" in {
      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      val newEmail = uniqueEmail
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(201)
      aVerificationEmailIsReceivedFor(email)

      val verificationResponse = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())
      verificationResponse.futureValue.status must be(200)
      verificationResponse must (have(bodyWith("Email address verified")) and
        have(bodyWith("You&#x27;ve now signed up for paperless notifications.")) and
        have(bodyWith("Continue to your HMRC online account")))

      `/preferences/terms-and-conditions`(authHelper.authHeader(utr)).postGenericOptOut.futureValue.status must be(200)

      val response = `/paperless/activate`(utr)().put().futureValue
      response.status must be(PRECONDITION_FAILED)
    }
    "return PRECONDITION_FAILED for Existing  Opted-out customer who was previously Opted-in with unverified email)" in {
      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(201)
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr)).postGenericOptOut.futureValue.status must be(200)

      val response = `/paperless/activate`(utr)().put().futureValue
      response.status must be(PRECONDITION_FAILED)
    }
  }

}

class NewActivateGraceInISpec extends EmailSupport with SessionCookieEncryptionSupport {
  private def additionalConfig =
    Map(
      "controllers.controllers.internal.ActivationController.needsAuth" -> false,
      "Test.activation.gracePeriodInMin"                                -> 10
    )

  override lazy val app = new GuiceApplicationBuilder()
    .configure(additionalConfig)
    .build()

  "activate within grace period" should {

    "return OK for Existing  Opted-out customer who was previously Opted-in with verified email)" in {
      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(201)
      aVerificationEmailIsReceivedFor(email)
      val verificationResponse = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())
      verificationResponse.futureValue.status must be(200)
      verificationResponse must (have(bodyWith("Email address verified")) and
        have(bodyWith("You&#x27;ve now signed up for paperless notifications.")) and
        have(bodyWith("Continue to your HMRC online account")))

      `/preferences/terms-and-conditions`(authHelper.authHeader(utr)).postGenericOptOut.futureValue.status must be(200)

      val response = `/paperless/activate`(utr)().put().futureValue
      response.status must be(OK)

    }
    "return OK for Existing  Opted-out customer who was previously Opted-in with unverified email)" in {
      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(201)
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr)).postGenericOptOut.futureValue.status must be(200)

      val response = `/paperless/activate`(utr)().put().futureValue
      response.status must be(OK)
    }

  }

}
