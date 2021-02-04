/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._

class NewActivateGraceOutISpec extends EmailSupport with SessionCookieEncryptionSupport {
  private def additionalConfig =
    Map(
      "controllers.controllers.internal.ActivationController.needsAuth" -> false,
      "Test.activation.gracePeriodInMin"                                -> 0,
      "play.http.router"                                                -> "legacy.Routes"
    )

  override lazy val app = new GuiceApplicationBuilder()
    .configure(additionalConfig)
    .build()

  "activate with grace period already passed" should {

    "return PRECONDITION_FAILED for existing PTA Customer who had previously opted out and has no email held in preferences" in {

      val utr = Generate.utr
      clearEmails()
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr)).postGenericOptOut.futureValue.status must be(
        CREATED)

      val response = `/paperless/activate`(utr)().put().futureValue
      response.status must be(PRECONDITION_FAILED)

    }

    "return OK for existing Opted-in customer with unverified email" in {

      val utr = Generate.utr
      clearEmails()
      val email = uniqueEmail
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(CREATED)

      val response = `/paperless/activate`(utr)().put().futureValue
      response.status must be(OK)
    }

    "return OK for Existing Opted-in customer with verified email" in {
      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(CREATED)
      aVerificationEmailIsReceivedFor(email)

      val verificationResponse = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())
      verificationResponse.futureValue.status must be(OK)

      verificationResponse must (have(bodyWith("Email address verified")) and
        have(bodyWith("You&#x27;ve now signed up for paperless notifications.")) and
        have(bodyWith("Continue to your HMRC online account")))

      val response = `/paperless/activate`(utr)().put().futureValue
      response.status must be(OK)
    }

    "set language preference based on cookie language value for Existing Opted-in customer which has no existing language preference" in {
      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(CREATED)
      aVerificationEmailIsReceivedFor(email)

      val entityId = `/entity-resolver/sa/:utr`(utr.value)
      `/preferences-admin/remove-language`(entityId)

      val prefStatusResponse = `/preferences`(authHelper.authHeader(utr)).getPreference.futureValue
      prefStatusResponse.status must be(OK)
      (prefStatusResponse.json \ "email" \ "language").isEmpty must be(true)

      val verificationResponse = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())
      verificationResponse.futureValue.status must be(OK)

      verificationResponse must (have(bodyWith("Email address verified")) and
        have(bodyWith("You&#x27;ve now signed up for paperless notifications.")) and
        have(bodyWith("Continue to your HMRC online account")))

      val response = `/paperless/activate`(utr)(None, None, Some("cy")).put().futureValue
      response.status must be(OK)

      val prefStatusActivatedResponse = `/preferences`(authHelper.authHeader(utr)).getPreference.futureValue
      prefStatusActivatedResponse.status must be(OK)
      (prefStatusActivatedResponse.json \ "email" \ "language").as[String] must be("cy")
    }

    "not silently overwrite a language preference based on cookie value for Existing Opted-in customer which has an existing language preference" in {
      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(CREATED)
      aVerificationEmailIsReceivedFor(email)

      val entityId = `/entity-resolver/sa/:utr`(utr.value)
      `/preferences-admin/remove-language`(entityId)

      val prefStatusResponse = `/preferences`(authHelper.authHeader(utr)).getPreference.futureValue
      prefStatusResponse.status must be(OK)
      (prefStatusResponse.json \ "email" \ "language").isEmpty must be(true)

      val verificationResponse = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())
      verificationResponse.futureValue.status must be(OK)

      verificationResponse must (have(bodyWith("Email address verified")) and
        have(bodyWith("You&#x27;ve now signed up for paperless notifications.")) and
        have(bodyWith("Continue to your HMRC online account")))

      val responseEnglish = `/paperless/activate`(utr)(None, None, Some("en")).put().futureValue
      responseEnglish.status must be(OK)

      val responseWelsh = `/paperless/activate`(utr)(None, None, Some("cy")).put().futureValue
      responseWelsh.status must be(OK)

      val prefStatusActivatedResponse = `/preferences`(authHelper.authHeader(utr)).getPreference.futureValue
      prefStatusActivatedResponse.status must be(OK)
      (prefStatusActivatedResponse.json \ "email" \ "language").as[String] must be("en")
    }

    "return PRECONDITION_FAILED for Existing Opted-out customer who was previously Opted-in with verified email" in {
      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      val newEmail = uniqueEmail
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(CREATED)
      aVerificationEmailIsReceivedFor(email)

      val verificationResponse = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())
      verificationResponse.futureValue.status must be(OK)
      verificationResponse must (have(bodyWith("Email address verified")) and
        have(bodyWith("You&#x27;ve now signed up for paperless notifications.")) and
        have(bodyWith("Continue to your HMRC online account")))

      `/preferences/terms-and-conditions`(authHelper.authHeader(utr)).postGenericOptOut.futureValue.status must be(OK)

      val response = `/paperless/activate`(utr)().put().futureValue
      response.status must be(PRECONDITION_FAILED)
    }
    "return PRECONDITION_FAILED for Existing Opted-out customer who was previously Opted-in with unverified email" in {
      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(CREATED)
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr)).postGenericOptOut.futureValue.status must be(OK)

      val response = `/paperless/activate`(utr)().put().futureValue
      response.status must be(PRECONDITION_FAILED)
    }
  }

}

class NewActivateGraceInISpec extends EmailSupport with SessionCookieEncryptionSupport {
  private def additionalConfig =
    Map(
      "controllers.controllers.internal.ActivationController.needsAuth" -> false,
      "play.http.router"                                                -> "legacy.Routes",
      "Test.activation.gracePeriodInMin"                                -> 10
    )

  override lazy val app = new GuiceApplicationBuilder()
    .configure(additionalConfig)
    .build()

  "activate within grace period" should {

    "return OK for Existing Opted-out customer who was previously Opted-in with verified email" in {
      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(CREATED)
      aVerificationEmailIsReceivedFor(email)
      val verificationResponse = `/sa/print-preferences/verification`.verify(verificationTokenFromEmail())
      verificationResponse.futureValue.status must be(OK)
      verificationResponse must (have(bodyWith("Email address verified")) and
        have(bodyWith("You&#x27;ve now signed up for paperless notifications."))) // and
      // have(bodyWith("Continue to your HMRC online account")))

      `/preferences/terms-and-conditions`(authHelper.authHeader(utr)).postGenericOptOut.futureValue.status must be(OK)

      val response = `/paperless/activate`(utr)().put().futureValue
      response.status must be(OK)

    }
    "return OK for Existing Opted-out customer who was previously Opted-in with unverified email" in {
      val utr = Generate.utr
      val email = uniqueEmail
      clearEmails()
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
        .postGenericOptIn(email)
        .futureValue
        .status must be(CREATED)
      `/preferences/terms-and-conditions`(authHelper.authHeader(utr)).postGenericOptOut.futureValue.status must be(OK)

      val response = `/paperless/activate`(utr)().put().futureValue
      response.status must be(OK)
    }

  }

}
