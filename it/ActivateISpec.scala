import play.api.test.Helpers._
import uk.gov.hmrc.domain.SaUtr

class ActivateISpec extends PreferencesFrontEndServer {

  "activate" should {
    "return PRECONDITION_FAILED with redirectUserTo link if activating sa-all for a new user" in new TestCaseWithFrontEndAuthentication {

      val response = `/paperless/activate/:form-type/:tax-identifier`("sa-all", SaUtr(utr))().put().futureValue
      response.status should be (PRECONDITION_FAILED)

      (response.json \ "redirectUserTo").as[String] should be (s"http://localhost:9024/paperless/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
    }

    "return PRECONDITION_FAILED with redirectUserTo link if activating notice-of-coding for a new user with given utr and nino" in new TestCaseWithFrontEndAuthentication {
      val nino = Generate.nino
      val response = `/paperless/activate/:form-type/:tax-identifier`("notice-of-coding", nino)(SaUtr(utr)).put().futureValue
      response.status should be (PRECONDITION_FAILED)
      (response.json \ "redirectUserTo").as[String] should be (s"http://localhost:9024/paperless/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
    }
  }
}
