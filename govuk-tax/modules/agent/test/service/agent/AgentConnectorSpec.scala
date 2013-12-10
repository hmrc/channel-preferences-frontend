package service.agent

import play.api.libs.json.JsValue
import play.api.test.WithApplication
import controllers.service.ResponseStub
import models.agent._
import models.agent.Client
import play.api.libs.ws.Response
import models.agent.PreferredContact
import models.agent.SearchRequest
import models.agent.MatchingPerson
import play.api.test.FakeApplication
import scala.Some
import uk.gov.hmrc.microservice.MicroServiceException
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.BaseSpec
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.Future

class AgentConnectorSpec extends BaseSpec with ScalaFutures {

  "The agent micro search service" should {
    "return the result when found" in new WithApplication(FakeApplication()) {
      val request = SearchRequest("exNino", Some("exFirst"), Some("exLast"), None)
      val service = new AgentConnector {
        override protected def httpPostF[A](uri: String, body: JsValue, headers: Map[String, String])(implicit m: Manifest[A], headerCarrier: HeaderCarrier): Future[Option[A]] = {
          Some(MatchingPerson("exNino", Some("exFirst"), Some("exLast"), Some("exDob"))).asInstanceOf[Some[A]]
        }
      }
      whenReady(service.searchClient("", request))(_ shouldBe Some(MatchingPerson("exNino", Some("exFirst"), Some("exLast"), Some("exDob"))))
    }

    "return none if not found" in new WithApplication(FakeApplication()) {
      val request = SearchRequest("unknown", Some("exFirst"), Some("exLast"), None)
      val service = new AgentConnector {
        override protected def httpPostF[A](uri: String, body: JsValue, headers: Map[String, String])(implicit m: Manifest[A], headerCarrier: HeaderCarrier): Future[Option[A]] = {
          None
        }
      }
      whenReady(service.searchClient("", request))(_ shouldBe None)
    }

  }
}
