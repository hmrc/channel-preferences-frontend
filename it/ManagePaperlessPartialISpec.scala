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

import java.util.UUID

class ManagePaperlessPartialISpec extends EmailSupport {

  "Manage Paperless partial" should {

    "return not authorised when no credentials supplied" in {
      `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue").get.futureValue.status must be(
        401)
    }

    "return opted out details when no preference is set" in {
      val request = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withHttpHeaders(cookieWithNino)
      val response = request.get().futureValue
      response.status must be(200)
      response.body must (
        include("Sign up for paperless notifications") and
          not include "You need to verify"
      )
    }
  }

  "Manage Paperless partial for pending verification" should {

    "contain pending email verification details" in {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val nino = Generate.nino
      val header = authHelper.authHeader(nino)
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(201)

      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withHttpHeaders(header)
        .get()
        .futureValue
      response.status must be(200)
      response.body must include(s"You need to verify")
    }

    "contain new email details for a subsequent change email" in {
      val utr = Generate.utr
      val header = authHelper.authHeader(utr)
      val email = s"${UUID.randomUUID().toString}@email.com"
      val newEmail = s"${UUID.randomUUID().toString}@email.com"
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences`(header).putPendingEmail(newEmail).futureValue.status must be(200)
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withHttpHeaders(header)
        .get()
        .futureValue
      response.status must be(200)
      checkForChangedEmailDetailsInResponse(response.body, email, newEmail, todayDate)
    }

    "contain sign up details for a subsequent opt out" in {
      val utr = Generate.utr
      val header = authHelper.authHeader(utr)
      val email = s"${UUID.randomUUID().toString}@email.com"
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences/terms-and-conditions`(header).postGenericOptOut.futureValue.status must be(200)
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withHttpHeaders(header)
        .get()
        .futureValue
      response.status must be(200)
      response.body must (
        not include email and
          include(s"Sign up for paperless notifications")
      )
    }
  }

  "Manage Paperless partial for verified user" should {

    "contain new email details for a subsequent change email" in {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val newEmail = s"${UUID.randomUUID().toString}@email.com"
      val utr = Generate.utr
      val header = authHelper.authHeader(utr)
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences-admin/sa/individual`.verifyEmailFor(`/entity-resolver/sa/:utr`(utr.value)).futureValue.status must be(
        204)
      `/preferences`(header).putPendingEmail(newEmail).futureValue.status must be(200)
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withHttpHeaders(header)
        .get()
        .futureValue
      response.status must be(200)
      checkForChangedEmailDetailsInResponse(response.body, email, newEmail, todayDate)
    }

    "contain sign up details for a subsequent opt out" in {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val nino = Generate.nino
      val header = authHelper.authHeader(nino)
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences-admin/sa/individual`.verifyEmailFor(`/entity-resolver/paye/:nino`(nino.value)).futureValue.status must be(
        204)
      `/preferences/terms-and-conditions`(header).postGenericOptOut().futureValue.status must be(200)
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withHttpHeaders(header)
        .get()
        .futureValue
      response.status must be(200)
      response.body must (not include email and
        include(s"Sign up for paperless notifications"))
    }
  }

  "Manage Paperless partial for a bounced verification email" should {

    "contain new email details for a subsequent change email" in {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val newEmail = s"${UUID.randomUUID().toString}@email.com"
      val utr = Generate.utr
      val header = authHelper.authHeader(utr)
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(204)
      `/preferences`(header).putPendingEmail(newEmail).futureValue.status must be(200)
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withHttpHeaders(header)
        .get()
        .futureValue
      response.status must be(200)
      checkForChangedEmailDetailsInResponse(response.body, email, newEmail, todayDate)
    }

    "contain sign up details for a subsequent opt out" in {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val utr = Generate.utr
      val header = authHelper.authHeader(utr)
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(204)
      `/preferences/terms-and-conditions`(header).postGenericOptOut.futureValue.status must be(200)
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withHttpHeaders(header)
        .get()
        .futureValue
      response.status must be(200)
      response.body must (not include email and
        include(s"Sign up for paperless notifications"))
    }

  }

  def checkForChangedEmailDetailsInResponse(
    response: String,
    oldEmail: String,
    newEmail: String,
    currentFormattedDate: String) =
    response must (include(s"You need to verify your email address.") and
      include(newEmail) and
      not include oldEmail and
      include(s"on $currentFormattedDate. Click on the link in the email to verify your email address."))
}
