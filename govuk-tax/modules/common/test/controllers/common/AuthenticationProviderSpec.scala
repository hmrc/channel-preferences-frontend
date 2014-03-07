package controllers.common

import uk.gov.hmrc.common.BaseSpec
import controllers.common.actions.HeaderCarrier
import play.api.test.{FakeRequest, WithApplication}
import org.scalatest.concurrent.ScalaFutures


class AuthenticationProviderSpec extends BaseSpec with ScalaFutures {


  "getTokenFromRequest" should {
    import IdaWithTokenCheckForBeta.getTokenFromRequest
    implicit val hc = HeaderCarrier

    "return None if there is no token parameter in the request query string" in new WithApplication {
      implicit val request = FakeRequest(method = "GET", path = "path")
      getTokenFromRequest shouldBe None
    }

    "return None if the token parameter in the request query string is empty" in {
      implicit val request = FakeRequest(method = "GET", path = "path?token=")
      getTokenFromRequest shouldBe None
    }

    "return the token in there is a non empty string token in the request query string" in {
      implicit val request = FakeRequest(method = "GET", path = "path?token=some-value")
      getTokenFromRequest shouldBe Some("some-value")
    }
  }

}
