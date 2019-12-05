package connectors

import _root_.helpers.ConfigHelper
import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.OneAppPerSuite
import play.api.Mode.Mode
import play.api.libs.json.{Json, Writes}
import play.api.{Application, Configuration, Play}
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.test._

import scala.concurrent.Future

class EmailConnectorSpec extends UnitSpec with ScalaFutures with OneAppPerSuite {

  implicit val hc = HeaderCarrier()
  override implicit lazy val app : Application = ConfigHelper.fakeApp

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
      val connector = new EmailConnector with HttpAuditing with ServicesConfig {
        protected def serviceUrl = "http://email.service:80"

        val hooks: Seq[HttpHook] = Seq(AuditingHook)

        def auditConnector = ???

        override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit wts: Writes[A], hc: HeaderCarrier): Future[HttpResponse]= Future.successful(responseFromEmailService)

        override def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier) = ???

        override def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier) = ???

        override def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier) = ???

        override protected def appNameConfiguration: Configuration = Play.current.configuration

        override protected def mode: Mode = Play.current.mode

        override protected def runModeConfiguration: Configuration = Play.current.configuration

        override protected def actorSystem: ActorSystem = Play.current.actorSystem

        override protected def configuration: Option[Config] = Some(Play.current.configuration.underlying)
      }

      val exampleEmailAddress = "bob@somewhere.com"
    }

    trait ServiceDownTestCase extends TestCase {
      def responseFromEmailService = ???
      override val connector = new EmailConnector with HttpAuditing with ServicesConfig {
        protected def serviceUrl = "http://email.service:80"
        
        val hooks: Seq[HttpHook] = Seq(AuditingHook)

        def auditConnector = ???

        override def configuration = Some(Play.current.configuration.underlying)

        override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit wts: Writes[A], hc: HeaderCarrier): Future[HttpResponse]= Future.failed(new Exception("Service down"))

        override def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier) = ???

        override def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier) = ???

        override def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier) = ???

        override protected def appNameConfiguration: Configuration = Play.current.configuration

        override protected def mode: Mode = Play.current.mode

        override protected def runModeConfiguration: Configuration = Play.current.configuration

        override protected def actorSystem: ActorSystem = Play.current.actorSystem
      }

    }
  }
}
