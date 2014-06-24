package connectors

import uk.gov.hmrc.domain.SaUtr
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.test.UnitSpec
import scala.concurrent.Future
import uk.gov.hmrc.play.connectors.HeaderCarrier

class PreferencesConnectorSpec extends UnitSpec with ScalaFutures {

  implicit val hc = new HeaderCarrier

  lazy val preferenceConnector = new TestPreferencesConnector()

  class TestPreferencesConnector extends PreferencesConnector {
    override def serviceUrl: String = ???

    override def http: HttpGet with HttpPost = ???
  }

  "The responseToEmailVerificationLinkStatus method" should {
    import EmailVerificationLinkResponse._

    "return ok if updateEmailValidationStatusUnsecured returns 200" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.successful(HttpResponse(200)))
      result.futureValue shouldBe OK
    }

    "return ok if updateEmailValidationStatusUnsecured returns 204" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.successful(HttpResponse(204)))
      result.futureValue shouldBe OK
    }

    "return error if updateEmailValidationStatusUnsecured returns 400" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(new BadRequestException("")))
      result.futureValue shouldBe ERROR
    }

    "return error if updateEmailValidationStatusUnsecured returns 404" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(new NotFoundException("")))
      result.futureValue shouldBe ERROR
    }

    "return error if updateEmailValidationStatusUnsecured returns 500" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(new Upstream5xxResponse("", 500, 500)))

      result.futureValue shouldBe ERROR
    }

    "return expired if updateEmailValidationStatusUnsecured returns 410" in {
      val result = preferenceConnector.responseToEmailVerificationLinkStatus(Future.failed(new Upstream4xxResponse("", 410, 500)))
      result.futureValue shouldBe EXPIRED
    }
  }
}
