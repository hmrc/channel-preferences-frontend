import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._

class ActivateISpec extends PreferencesFrontEndServer with EmailSupport with MockitoSugar {

  "activate" should {
    "return PRECONDITION_FAILED with redirectUserTo link if activating sa-all for a new user" in new TestCaseWithFrontEndAuthentication {
      val saUtr = Generate.utr

      val response = `/paperless/activate/:form-type/:tax-identifier`("sa-all", saUtr)().put().futureValue
      response.status should be (PRECONDITION_FAILED)

      (response.json \ "redirectUserTo").as[String] should be (s"/paperless/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
    }

    "return PRECONDITION_FAILED with redirectUserTo link if activating notice-of-coding for a new user with given utr and nino" in new TestCaseWithFrontEndAuthentication {
      val nino = Generate.nino
      val saUtr = Generate.utr
      val response = `/paperless/activate/:form-type/:tax-identifier`("notice-of-coding", nino)(saUtr).put().futureValue
      response.status should be (PRECONDITION_FAILED)
      (response.json \ "redirectUserTo").as[String] should be (s"/paperless/choose?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnText")
    }
  }
}
