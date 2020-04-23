/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

import java.util.UUID

import uk.gov.hmrc.http.SessionKeys
import play.api.test.Helpers._

class ManagePaperlessPartialISpec extends EmailSupport with SessionCookieEncryptionSupport {

  "Manage Paperless partial" should {

    "return not authorised when no credentials supplied" in {
      `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue").get.futureValue.status must be(
        UNAUTHORIZED)
    }

    "return opted out details when no preference is set" in {

      val request = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> ggAuthHeaderWithNino._2)
        )()
      val response = request.get().futureValue
      response.status must be(OK)
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
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(CREATED)

      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> header._2)
        )()
        .get()
        .futureValue
      response.status must be(OK)
      response.body must include(s"You need to verify")
    }

    "contain new email details for a subsequent change email" in {
      val utr = Generate.utr
      val header = authHelper.authHeader(utr)
      val email = s"${UUID.randomUUID().toString}@email.com"
      val newEmail = s"${UUID.randomUUID().toString}@email.com"
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(CREATED)
      `/preferences`(header).putPendingEmail(newEmail).futureValue.status must be(OK)
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> header._2)
        )()
        .get()
        .futureValue
      response.status must be(OK)
      checkForChangedEmailDetailsInResponse(response.body, email, newEmail, todayDate)
    }

    "contain sign up details for a subsequent opt out" in {
      val utr = Generate.utr
      val header = authHelper.authHeader(utr)
      val email = s"${UUID.randomUUID().toString}@email.com"
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(CREATED)
      `/preferences/terms-and-conditions`(header).postGenericOptOut.futureValue.status must be(OK)
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> header._2)
        )()
        .get()
        .futureValue
      response.status must be(OK)
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
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(CREATED)
      `/preferences-admin/sa/individual`.verifyEmailFor(`/entity-resolver/sa/:utr`(utr.value)).futureValue.status must be(
        NO_CONTENT)
      `/preferences`(header).putPendingEmail(newEmail).futureValue.status must be(OK)
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> header._2)
        )()
        .get()
        .futureValue
      response.status must be(OK)
      checkForChangedEmailDetailsInResponse(response.body, email, newEmail, todayDate)
    }

    "contain sign up details for a subsequent opt out" in {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val nino = Generate.nino
      val header = authHelper.authHeader(nino)
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(CREATED)
      `/preferences-admin/sa/individual`.verifyEmailFor(`/entity-resolver/paye/:nino`(nino.value)).futureValue.status must be(
        NO_CONTENT)
      `/preferences/terms-and-conditions`(header).postGenericOptOut().futureValue.status must be(OK)
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> header._2)
        )()
        .get()
        .futureValue
      response.status must be(OK)
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
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(CREATED)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(NO_CONTENT)
      `/preferences`(header).putPendingEmail(newEmail).futureValue.status must be(OK)
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> header._2)
        )()
        .get()
        .futureValue
      response.status must be(OK)
      checkForChangedEmailDetailsInResponse(response.body, email, newEmail, todayDate)
    }

    "contain sign up details for a subsequent opt out" in {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val utr = Generate.utr
      val header = authHelper.authHeader(utr)
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(CREATED)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(NO_CONTENT)
      `/preferences/terms-and-conditions`(header).postGenericOptOut.futureValue.status must be(OK)
      val response = `/paperless/manage`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> header._2)
        )()
        .get()
        .futureValue
      response.status must be(OK)
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
