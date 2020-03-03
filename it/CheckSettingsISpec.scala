/*
 * Copyright 2019 HM Revenue & Customs
 *
 */

import java.util.UUID

import uk.gov.hmrc.http.SessionKeys
import org.jsoup.Jsoup

class CheckSettingsISpec extends EmailSupport with SessionCookieEncryptionSupport {

  "Check Settings" should {

    "return not authorised when no credentials supplied" in {
      `/paperless/check-settings`(returnUrl = "http://some/other/url", returnLinkText = "Continue").get.futureValue.status must be(
        401
      )
    }

    "return rendered digital_false_full" in {

      val request = `/paperless/check-settings`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> ggAuthHeaderWithNino._2)
        )
      val response = request.get().futureValue
      response.status must be(200)

      val document =
        Jsoup.parse(response.body)

      document.getElementById("saCheckSettings").text() mustBe "Check your settings"
      document.getElementsByClass("govuk-link").first().attr("href") must include("/paperless/choose")
    }
  }

  "Check Settings for pending verification" should {

    "contain pending email verification details" in {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val nino = Generate.nino
      val header = authHelper.authHeader(nino)
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(201)

      val response = `/paperless/check-settings`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> header._2)
        )
        .get()
        .futureValue
      response.status must be(200)

      response.body must include(s"You need to verify your email address before you can receive tax documents online")
      response.body must include(email)
    }

    "contain new email details for a subsequent change email" in {
      val utr = Generate.utr
      val header = authHelper.authHeader(utr)
      val email = s"${UUID.randomUUID().toString}@email.com"
      val newEmail = s"${UUID.randomUUID().toString}@email.com"
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences`(header).putPendingEmail(newEmail).futureValue.status must be(200)
      val response = `/paperless/check-settings`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> header._2)
        )
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
      val response = `/paperless/check-settings`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> header._2)
        )
        .get()
        .futureValue

      response.status must be(200)
      response.body must (
        not include email
          and
            include(s"Post")
      )
    }
  }

  "Check settings for verified user" should {

    "contain tax document message and verified email address" in {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val utr = Generate.utr
      val header = authHelper.authHeader(utr)
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences-admin/sa/individual`.verifyEmailFor(`/entity-resolver/sa/:utr`(utr.value)).futureValue.status must be(
        204
      )
      val response = `/paperless/check-settings`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> header._2)
        )
        .get()
        .futureValue

      response.status must be(200)
      response.body must include(s"Online messages, or post when not available")
      response.body must include(email)
    }

    "contain new email details for a subsequent change email" in {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val newEmail = s"${UUID.randomUUID().toString}@email.com"
      val utr = Generate.utr
      val header = authHelper.authHeader(utr)
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences-admin/sa/individual`.verifyEmailFor(`/entity-resolver/sa/:utr`(utr.value)).futureValue.status must be(
        204
      )
      `/preferences`(header).putPendingEmail(newEmail).futureValue.status must be(200)
      val response = `/paperless/check-settings`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> header._2)
        )
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
        204
      )
      `/preferences/terms-and-conditions`(header).postGenericOptOut().futureValue.status must be(200)
      val response = `/paperless/check-settings`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> header._2)
        )
        .get()
        .futureValue
      response.status must be(200)
      response.body must (
        not include email
          and
            include(s"Post")
      )
    }
  }

  "Check settings for a bounced email" should {

    "contain warning message and email address" in {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val utr = Generate.utr
      val header = authHelper.authHeader(utr)
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(204)

      val response = `/paperless/check-settings`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> header._2)
        )
        .get()
        .futureValue

      response.status must be(200)
      response.body must include(s"We could not send an email to the address you entered")
      response.body must include(s"Check and fix your email address or account")
      response.body must include(email)
    }

    "contain new email details for a subsequent change email" in {
      val email = s"${UUID.randomUUID().toString}@email.com"
      val newEmail = s"${UUID.randomUUID().toString}@email.com"
      val utr = Generate.utr
      val header = authHelper.authHeader(utr)
      `/preferences/terms-and-conditions`(header).postGenericOptIn(email).futureValue.status must be(201)
      `/preferences-admin/bounce-email`.post(email).futureValue.status must be(204)
      `/preferences`(header).putPendingEmail(newEmail).futureValue.status must be(200)
      val response = `/paperless/check-settings`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> header._2)
        )
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
      val response = `/paperless/check-settings`(returnUrl = "http://some/other/url", returnLinkText = "Continue")
        .withSession(
          (SessionKeys.authToken -> header._2)
        )
        .get()
        .futureValue
      response.status must be(200)
      response.body must (
        not include email
          and
            include(s"Post")
      )
    }

  }

  def checkForChangedEmailDetailsInResponse(
    response: String,
    oldEmail: String,
    newEmail: String,
    currentFormattedDate: String
  ) =
    response must (include(s"You need to verify your email address") and
      include(newEmail) and
      not include oldEmail and
      include(s"Verify your email address"))
}
