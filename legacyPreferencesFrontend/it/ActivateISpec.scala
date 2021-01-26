/*
 * Copyright 2019 HM Revenue & Customs
 *
 */

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._

import java.util.UUID

class ActivateISpec extends EmailSupport with SessionCookieEncryptionSupport {
  private def additionalConfig =
    Map(
      "controllers.controllers.internal.ActivationController.needsAuth" -> false,
      "play.http.router"                                                -> "preferences_frontend.Routes"
    )

  override lazy val app = new GuiceApplicationBuilder()
    .configure(additionalConfig)
    .build()

  "activate" should {
    //   "return PRECONDITION_FAILED with redirectUserTo link if activating for a new user with utr only" in {
    //     val response = `/paperless/activate`(utr)().put().futureValue
    //     response.status must be(PRECONDITION_FAILED)
    //     (response.json \ "redirectUserTo").as[String] must be(
    //       s"http://localhost:9053/paperless/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
    //     (response.json \ "optedIn").asOpt[Boolean] mustBe empty
    //     (response.json \ "verifiedEmail").asOpt[Boolean] mustBe empty
    //   }

    //   "return PRECONDITION_FAILED with redirectUserTo link if activating for a new user with nino only for taxCredits" in {
    //     val termsAndConditions = "taxCredits"
    //     val emailAddress = "test@test.com"
    //     val response = `/paperless/activate`(nino)(Some(termsAndConditions), Some(emailAddress)).put().futureValue
    //     response.status must be(PRECONDITION_FAILED)
    //     (response.json \ "redirectUserTo").as[String] must be(
    //       s"http://localhost:9053/paperless/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText&termsAndConditions=${encryptAndEncode(
    //         termsAndConditions)}&email=${encryptAndEncode(emailAddress)}")
    //     (response.json \ "optedIn").asOpt[Boolean] mustBe empty
    //     (response.json \ "verifiedEmail").asOpt[Boolean] mustBe empty
    //   }

    //   "return BAD_REQUEST with activating for a new user with nino only for taxCredits without providing email" in {
    //     val response =
    //       `/paperless/activate`(nino)(termsAndConditions = Some("taxCredits"), emailAddress = None).put().futureValue
    //     response.status must be(BAD_REQUEST)
    //   }

    //   "return PRECONDITION_FAILED with redirectUserTo link if activating for a new user with given utr and nino" in {
    //     val response = `/paperless/activate`(nino, utr)().put().futureValue
    //     response.status must be(PRECONDITION_FAILED)
    //     (response.json \ "redirectUserTo").as[String] must be(
    //       s"http://localhost:9053/paperless/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
    //     (response.json \ "optedIn").asOpt[Boolean] mustBe empty
    //     (response.json \ "verifiedEmail").asOpt[Boolean] mustBe empty
    //   }

    //   "return UNAUTHORIZED if activating for a user with no nino or utr" in {
    //     val response = `/paperless/activate`()().put().futureValue
    //     response.status must be(UNAUTHORIZED)
    //   }

    //   "return PRECONDITION_FAILED with redirectUserTo link if activating for a new user with nino only" in {
    //     val response = `/paperless/activate`(nino)().put().futureValue
    //     response.status must be(PRECONDITION_FAILED)
    //     (response.json \ "redirectUserTo").as[String] must be(
    //       s"http://localhost:9053/paperless/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
    //     (response.json \ "optedIn").asOpt[Boolean] mustBe empty
    //     (response.json \ "verifiedEmail").asOpt[Boolean] mustBe empty
    //   }

    "return OK with the optedIn attribute set to true and verifiedEmail set to false if the user has opted in and not verified" in {

      val email = s"${UUID.randomUUID().toString}@email.com"

      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email).futureValue.status must be(
        CREATED
      )

      val response = `/paperless/activate`(utr)().put().futureValue
      response.status must be(OK)
      (response.json \ "optedIn").as[Boolean] mustBe true
      (response.json \ "verifiedEmail").as[Boolean] mustBe false
      (response.json \ "redirectUserTo").asOpt[String] mustBe empty
    }

    // "return OK with the optedIn attribute set to true and verifiedEmail set to true if the user has opted in and verified" in {

    //   val utr = Generate.utr
    //   val email = s"${UUID.randomUUID().toString}@email.com"

    //   `/preferences/terms-and-conditions`(authHelper.authHeader(utr))
    //     .postGenericOptIn(email)
    //     .futureValue
    //     .status must be(CREATED)
    //   `/preferences-admin/sa/individual`.verifyEmailFor(`/entity-resolver/sa/:utr`(utr.value)).futureValue.status must be(
    //     NO_CONTENT)
    //   val response = `/paperless/activate`(utr)().put().futureValue
    //   response.status must be(OK)
    //   (response.json \ "optedIn").as[Boolean] mustBe true
    //   (response.json \ "verifiedEmail").as[Boolean] mustBe true
    //   (response.json \ "redirectUserTo").asOpt[String] mustBe empty
    // }

    // "return OK with the optedId attribute set to false if the user has opted out" in {

    //   val utr = Generate.utr
    //   val email = s"${UUID.randomUUID().toString}@email.com"
    //   `/preferences/terms-and-conditions`(authHelper.authHeader(utr)).postGenericOptOut.futureValue.status must be(
    //     CREATED)

    //   val response = `/paperless/activate`(utr)().put().futureValue
    //   response.status must be(OK)
    //   (response.json \ "optedIn").as[Boolean] mustBe false
    //   (response.json \ "verifiedEmail").asOpt[Boolean] mustBe empty
    //   (response.json \ "redirectUserTo").asOpt[String] mustBe empty

    // }

    // "return CONFLICT if trying to activate providing an email different than the stored one" in {
    //   val originalEmail = "generic@test.com"

    //   `/preferences/terms-and-conditions`(ggAuthHeaderWithNino)
    //     .postGenericOptIn(originalEmail)
    //     .futureValue
    //     .status must be(CREATED)

    //   `/paperless/activate`(nino)(Some("taxCredits"), Some("taxCredits@test.com")).put().futureValue.status must be(
    //     CONFLICT)
    // }
  }
}
