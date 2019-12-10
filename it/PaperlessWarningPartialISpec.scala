import uk.gov.hmrc.http.SessionKeys

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

class PaperlessWarningPartialISpec extends EmailSupport with SessionCookieEncryptionSupport {

  "Paperless warnings partial " should {
    "return not authorised when no credentials supplied" in new TestCase {
      `/paperless/warnings`.get().futureValue.status must be(401)
    }

    "be not found if the user has no preferences with utr only" in new TestCase {
      val response = `/paperless/warnings`.withSession((SessionKeys.authToken -> cookieWithUtr._2))
        .get()
        .futureValue

      response.status must be(404)
    }

    "be not found if the user has no preferences with nino only" in new TestCase {
      val response = `/paperless/warnings`.withSession((SessionKeys.authToken -> cookieWithNino._2))
        .get()
        .futureValue

      response.status must be(404)
    }
  }

  "Paperless warnings partial for verification pending" should {

    "have a verification warning for the unverified email" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      ).get()
        .futureValue

      response.status must be(200)
      response.body must include(s"Verify your email address for paperless notifications")
    }

    "have no warning if user then verifies email" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences-admin/sa/individual`.verifyEmailFor(`/entity-resolver/sa/:utr`(utr.value)).futureValue.status must be(
        204)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      ).get()
        .futureValue

      response.status must be(200)
      response.body must be("")
    }

    "have no warning if user then opts out" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptOut.futureValue.status must be(200)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      ).get()
        .futureValue

      response.status must be(200)
      response.body must be("")
    }

    "have verification warning if user then changes email" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(changedUniqueEmail).futureValue.status must be(200)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      ).get()
        .futureValue

      response.status must be(200)
      response.body must include("Verify your email address for paperless notifications")
    }
  }

  "Paperless warnings partial for a bounced unverified email address" should {

    "have a bounced warning" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(204)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      ).get()
        .futureValue

      response.status must be(200)
      response.body must include("There&#x27;s a problem with your paperless notification emails")
    }

    "have no warning if user then opts out" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(204)
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptOut

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      ).get()
        .futureValue

      response.status must be(200)
      response.body must be("")
    }

    "have a verification warning if user then successfully sends verification link to same address" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(204)
      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(email).futureValue.status must be(200)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      ).get()
        .futureValue

      response.status must be(200)
      response.body must include("Verify your email address for paperless notifications")
    }

    "have verification warning if user then changes email" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(204)
      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(changedUniqueEmail).futureValue.status must be(200)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      ).get()
        .futureValue

      response.status must be(200)
      response.body must include("Verify your email address for paperless notifications")
    }

    "have no warning if user successfully resends link and verifies" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(204)
      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(email).futureValue.status must be(200)
      `/preferences-admin/sa/individual`.verifyEmailFor(`/entity-resolver/sa/:utr`(utr.value)).futureValue.status must be(
        204)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      ).get()
        .futureValue

      response.status must be(200)
      response.body must be("")
    }

    "have inbox full warning if user resends link and their inbox is full" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(204)
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(200)
      `/preferences-admin/sa/bounce-email-inbox-full`.post(email).futureValue.status must be(204)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      ).get()
        .futureValue

      response.status must be(200)
      response.body must include("Your inbox is full")
    }

  }

  "Paperless warnings partial for opted out user" should {

    "be empty" in new TestCase {
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptOut.futureValue.status must be(201)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      ).get()
        .futureValue

      response.status must be(200)
    }
  }

  "Paperless warnings partial for a bounced verified email address" should {

    "have a bounced warning" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences-admin/sa/individual`.verifyEmailFor(`/entity-resolver/sa/:utr`(utr.value)).futureValue.status must be(
        204)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(204)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      ).get()
        .futureValue

      response.status must be(200)
      response.body must include("There&#x27;s a problem with your paperless notification emails")
    }
  }
  trait TestCase {
    val utr = Generate.utr
    val nino = Generate.nino
    val ggAuthHeaderWithUtr = authHelper.authHeader(utr)
    val ggAuthHeaderWithNino = authHelper.authHeader(nino)
    val cookieWithUtr = ggAuthHeaderWithUtr
    val cookieWithNino = ggAuthHeaderWithNino

  }
}
