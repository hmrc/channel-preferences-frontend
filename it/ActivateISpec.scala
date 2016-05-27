import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Vrn, CtUtr, SaUtr}

class ActivateISpec extends PreferencesFrontEndServer {

  "activate" should {
    "return PRECONDITION_FAILED with redirectUserTo link if activating sa-all for a new user with utr only" in new TestCaseWithFrontEndAuthentication {

      val response = `/paperless/activate/:form-type/:tax-identifier`("sa-all", SaUtr(utr))().put().futureValue
      response.status should be (PRECONDITION_FAILED)

      (response.json \ "redirectUserTo").as[String] should be (s"http://localhost:9024/paperless/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
    }

    "return PRECONDITION_FAILED with redirectUserTo link if activating notice-of-coding for a new user with given utr and nino" in new TestCaseWithFrontEndAuthentication {
      val response = `/paperless/activate/:form-type/:tax-identifier`("notice-of-coding", nino)(SaUtr(utr)).put().futureValue
      response.status should be (PRECONDITION_FAILED)
      (response.json \ "redirectUserTo").as[String] should be (s"http://localhost:9024/paperless/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
    }

    "return PRECONDITION_FAILED with redirectUserTo link if activating for a user with no nino or utr" in new TestCaseWithFrontEndAuthentication {
      val response = `/paperless/activate/:form-type/:tax-identifier`("notice-of-coding", CtUtr("TEST_USER"))(Vrn("ANOTHER_TEST")).put().futureValue
      response.status should be (UNAUTHORIZED)
    }


    "return PRECONDITION_FAILED with redirectUserTo link if activating for a new user with nino only" in new TestCaseWithFrontEndAuthentication {
      val response = `/paperless/activate/:form-type/:tax-identifier`("notice-of-coding", nino)().put().futureValue
      response.status should be (PRECONDITION_FAILED)
      (response.json \ "redirectUserTo").as[String] should be (s"http://localhost:9024/paperless/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
    }
  }
}
