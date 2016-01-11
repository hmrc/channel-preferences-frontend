package connectors

import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test._

import scala.concurrent.Future

class EmailConnectorSpec extends UnitSpec with ScalaFutures with WithFakeApplication {

  implicit val hc = HeaderCarrier()

  "Validating an email address" should {

    "return true if the service returns true" in new TestCase {
      val responseFromEmailService = HttpResponse(responseStatus = 200, responseJson = Some(Json.obj("valid" -> true)))
      connector.isValid(exampleEmailAddress).futureValue shouldBe true
    }

    "return false if the service returns false" in new TestCase {
      val responseFromEmailService = HttpResponse(responseStatus = 200, responseJson = Some(Json.obj("valid" -> false)))
      connector.isValid(exampleEmailAddress).futureValue shouldBe false
    }

    "returns false if service unavailable" in new ServiceDownTestCase {
      connector.isValid(exampleEmailAddress).futureValue shouldBe false
    }

    trait TestCase {
      def responseFromEmailService: HttpResponse
      val connector = new EmailConnector with HttpAuditing {
        protected def serviceUrl = "http://email.service:80"

        protected def doGet(url: String)(implicit hc: HeaderCarrier) = {
          Future.successful(responseFromEmailService)
        }

        val hooks: Seq[HttpHook] = Seq(AuditingHook)

        def auditConnector = ???
      }

      val exampleEmailAddress = "bob@somewhere.com"
    }

    trait ServiceDownTestCase extends TestCase {
      def responseFromEmailService = ???
      override val connector = new EmailConnector with HttpAuditing {
        protected def serviceUrl = "http://email.service:80"

        protected def doGet(url: String)(implicit hc: HeaderCarrier) = {
          Future.failed(new Exception("Service down"))
        }

        val hooks: Seq[HttpHook] = Seq(AuditingHook)

        def auditConnector = ???
      }

    }
  }
}