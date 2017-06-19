import java.util.UUID

import play.api.test.Helpers._
import uk.gov.hmrc.domain.{CtUtr, Vrn}

class ActivateISpec extends PreferencesFrontEndServer with EmailSupport {

  "activate" should {
    "return PRECONDITION_FAILED with redirectUserTo link if activating for a new user with utr only" in new TestCaseWithFrontEndAuthentication {
      val response = `/paperless/activate`(utr)().put().futureValue
      response.status should be (PRECONDITION_FAILED)
      (response.json \ "redirectUserTo").as[String] should be (s"http://localhost:9024/paperless/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
      (response.json \ "optedIn").asOpt[Boolean] shouldBe empty
      (response.json \ "verifiedEmail").asOpt[Boolean] shouldBe empty
    }

    "return PRECONDITION_FAILED with redirectUserTo link if activating for a new user with nino only for taxCredits" in new TestCaseWithFrontEndAuthentication {
      val termsAndConditions = "taxCredits"
      val emailAddress = "test@test.com"
      val response = `/paperless/activate`(nino)(Some(termsAndConditions), Some(emailAddress)).put().futureValue
      response.status should be (PRECONDITION_FAILED)
      (response.json \ "redirectUserTo").as[String] should be (s"http://localhost:9024/paperless/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText&termsAndConditions=${encryptAndEncode(termsAndConditions)}&email=${encryptAndEncode(emailAddress)}")
      (response.json \ "optedIn").asOpt[Boolean] shouldBe empty
      (response.json \ "verifiedEmail").asOpt[Boolean] shouldBe empty
    }

    "return BAD_REQUEST with activating for a new user with nino only for taxCredits without providing email" in new TestCaseWithFrontEndAuthentication {
      val response = `/paperless/activate`(nino)(termsAndConditions = Some("taxCredits"), emailAddress = None).put().futureValue
      response.status should be (BAD_REQUEST)
    }

    "return PRECONDITION_FAILED with redirectUserTo link if activating for a new user with given utr and nino" in new TestCaseWithFrontEndAuthentication {
      val response = `/paperless/activate`(nino, utr)().put().futureValue
      response.status should be (PRECONDITION_FAILED)
      (response.json \ "redirectUserTo").as[String] should be (s"http://localhost:9024/paperless/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
      (response.json \ "optedIn").asOpt[Boolean] shouldBe empty
      (response.json \ "verifiedEmail").asOpt[Boolean] shouldBe empty
    }

    "return UNAUTHORIZED if activating for a user with no nino or utr" in new TestCaseWithFrontEndAuthentication {
      val response = `/paperless/activate`(CtUtr("TEST_USER"), Vrn("ANOTHER_TEST"))().put().futureValue
      response.status should be (UNAUTHORIZED)
    }

    "return PRECONDITION_FAILED with redirectUserTo link if activating for a new user with nino only" in new TestCaseWithFrontEndAuthentication {
      val response = `/paperless/activate`(nino)().put().futureValue
      response.status should be (PRECONDITION_FAILED)
      (response.json \ "redirectUserTo").as[String] should be (s"http://localhost:9024/paperless/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
      (response.json \ "optedIn").asOpt[Boolean] shouldBe empty
      (response.json \ "verifiedEmail").asOpt[Boolean] shouldBe empty
    }

    "return OK with the optedIn attribute set to true and verifiedEmail set to false if the user has opted in and not verified" in new TestCaseWithFrontEndAuthentication {

      val email = s"${UUID.randomUUID().toString}@email.com"
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email) should have(status(201))

      val response = `/paperless/activate`(utr)().put().futureValue
      response.status should be (OK)
      (response.json \ "optedIn").as[Boolean] shouldBe true
      (response.json \ "verifiedEmail").as[Boolean] shouldBe false
      (response.json \ "redirectUserTo").asOpt[String] shouldBe empty

    }

    "return OK with the optedIn attribute set to true and verifiedEmail set to true if the user has opted in and verified" in new TestCaseWithFrontEndAuthentication {

      val email = s"${UUID.randomUUID().toString}@email.com"
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptIn(email) should have(status(201))
      `/preferences-admin/sa/individual`.verifyEmailFor(`/entity-resolver/sa/:utr`(utr.value)) should have(status(204))
      val response = `/paperless/activate`(utr)().put().futureValue
      response.status should be (OK)
      (response.json \ "optedIn").as[Boolean] shouldBe true
      (response.json \ "verifiedEmail").as[Boolean] shouldBe true
      (response.json \ "redirectUserTo").asOpt[String] shouldBe empty

    }

    "return OK with the optedId attribute set to false if the user has opted out" in new TestCaseWithFrontEndAuthentication {

      val email = s"${UUID.randomUUID().toString}@email.com"
      `/preferences/terms-and-conditions`(ggAuthHeaderWithUtr).postGenericOptOut should have(status(201))

      val response = `/paperless/activate`(utr)().put().futureValue
      response.status should be (OK)
      (response.json \ "optedIn").as[Boolean] shouldBe false
      (response.json \ "verifiedEmail").asOpt[Boolean] shouldBe empty
      (response.json \ "redirectUserTo").asOpt[String] shouldBe empty

    }
  }
}
