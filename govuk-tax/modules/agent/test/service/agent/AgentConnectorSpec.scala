package service.agent

import play.api.test.WithApplication
import models.agent.SearchRequest
import models.agent.MatchingPerson
import play.api.test.FakeApplication
import scala.Some
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.BaseSpec

import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.Future

class AgentConnectorSpec extends BaseSpec with ScalaFutures {

  "The agent micro search service" should {
    "return the result when found" in new WithApplication(FakeApplication()) {
      val request = SearchRequest("exNino", Some("exFirst"), Some("exLast"), None)
      val service = new AgentConnector {
        override protected def httpPostF[A, B](uri: String, body: A, headers: Map[String, String] = Map.empty)(implicit a: Manifest[A], b: Manifest[B], headerCarrier: HeaderCarrier): Future[Option[B]] = {
          Some(MatchingPerson("exNino", Some("exFirst"), Some("exLast"), Some("exDob"))).asInstanceOf[Some[B]]
        }
      }
      whenReady(service.searchClient("", request))(_ shouldBe Some(MatchingPerson("exNino", Some("exFirst"), Some("exLast"), Some("exDob"))))
    }

    "return none if not found" in new WithApplication(FakeApplication()) {
      val request = SearchRequest("unknown", Some("exFirst"), Some("exLast"), None)
      val service = new AgentConnector {
        override protected def httpPostF[A, B](uri: String, body: A, headers: Map[String, String] = Map.empty)(implicit a: Manifest[A], b: Manifest[B], headerCarrier: HeaderCarrier): Future[Option[B]] = {
          Future.successful(None)
        }
      }
      whenReady(service.searchClient("", request))(_ shouldBe None)
    }

  }
}
