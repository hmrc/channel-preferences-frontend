/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

import uk.gov.hmrc.http.SessionKeys
import play.api.test.Helpers._

class PaperlessWarningPartialISpec extends EmailSupport with SessionCookieEncryptionSupport {

  "Paperless warnings partial " should {
    "return not authorised when no credentials supplied" in new TestCase {
      `/paperless/warnings`.get().futureValue.status must be(UNAUTHORIZED)
    }

    "be not found if the user has no preferences with utr only" in new TestCase {
      val response = `/paperless/warnings`.withSession((SessionKeys.authToken -> cookieWithUtr._2))()
        .get()
        .futureValue

      response.status must be(NOT_FOUND)
    }

    "be not found if the user has no preferences with nino only" in new TestCase {
      val response = `/paperless/warnings`.withSession((SessionKeys.authToken -> cookieWithNino._2))()
        .get()
        .futureValue

      response.status must be(NOT_FOUND)
    }
  }

  "Paperless warnings partial for verification pending" should {

    "have a verification warning for the unverified email" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(
        CREATED)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      )().get().futureValue

      response.status must be(OK)
      response.body must include(s"Verify your email address for paperless notifications")
    }

    "have no warning if user then verifies email" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(
        CREATED)
      `/preferences-admin/sa/individual`.verifyEmailFor(`/entity-resolver/sa/:utr`(utr.value)).futureValue.status must be(
        NO_CONTENT)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      )().get().futureValue

      response.status must be(OK)
      response.body must be("")
    }

    "have no warning if user then opts out" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(
        CREATED)
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptOut.futureValue.status must be(OK)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      )().get().futureValue

      response.status must be(OK)
      response.body must be("")
    }

    "have verification warning if user then changes email" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(
        CREATED)
      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(changedUniqueEmail).futureValue.status must be(OK)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      )().get().futureValue

      response.status must be(OK)
      response.body must include("Verify your email address for paperless notifications")
    }
  }

  "Paperless warnings partial for a bounced unverified email address" should {

    "have a bounced warning" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(
        CREATED)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(NO_CONTENT)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      )().get().futureValue

      response.status must be(OK)
      response.body must include("There&#x27;s a problem with your paperless notification emails")
    }

    "have no warning if user then opts out" ignore new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(
        CREATED)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(NO_CONTENT)
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptOut

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      )().get().futureValue

      response.status must be(OK)
      response.body must be("")
    }

    "have a verification warning if user then successfully sends verification link to same address" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(
        CREATED)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(NO_CONTENT)
      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(email).futureValue.status must be(OK)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      )().get().futureValue

      response.status must be(OK)
      response.body must include("Verify your email address for paperless notifications")
    }

    "have verification warning if user then changes email" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(
        CREATED)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(NO_CONTENT)
      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(changedUniqueEmail).futureValue.status must be(OK)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      )().get().futureValue

      response.status must be(OK)
      response.body must include("Verify your email address for paperless notifications")
    }

    "have no warning if user successfully resends link and verifies" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(
        CREATED)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(NO_CONTENT)
      `/preferences`(ggAuthHeaderWithUtr).putPendingEmail(email).futureValue.status must be(OK)
      `/preferences-admin/sa/individual`.verifyEmailFor(`/entity-resolver/sa/:utr`(utr.value)).futureValue.status must be(
        NO_CONTENT)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      )().get().futureValue

      response.status must be(OK)
      response.body must be("")
    }

    "have inbox full warning if user resends link and their inbox is full" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(
        CREATED)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(NO_CONTENT)
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(OK)
      `/preferences-admin/sa/bounce-email-inbox-full`.post(email).futureValue.status must be(NO_CONTENT)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      )().get().futureValue

      response.status must be(OK)
      response.body must include("Your inbox is full")
    }

  }

  "Paperless warnings partial for opted out user" should {

    "be empty" in new TestCase {
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptOut.futureValue.status must be(CREATED)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      )().get().futureValue

      response.status must be(OK)
    }
  }

  "Paperless warnings partial for a bounced verified email address" should {

    "have a bounced warning" in new TestCase {
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(
        CREATED)
      `/preferences-admin/sa/individual`.verifyEmailFor(`/entity-resolver/sa/:utr`(utr.value)).futureValue.status must be(
        NO_CONTENT)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(NO_CONTENT)

      val response = `/paperless/warnings`.withSession(
        (SessionKeys.authToken -> cookieWithUtr._2)
      )().get().futureValue

      response.status must be(OK)
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
