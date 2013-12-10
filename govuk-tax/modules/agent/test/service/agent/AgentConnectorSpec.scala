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
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures

class AgentConnectorSpec extends BaseSpec with ScalaFutures {

  "The agent micro search service" should {
    "return the result when found" in new WithApplication(FakeApplication()) {
      val request = SearchRequest("exNino", Some("exFirst"), Some("exLast"), None)
      val service = new AgentConnector {
        override protected def httpPostF[A](uri: String, body: JsValue, headers: Map[String, String])(implicit m: Manifest[A], headerCarrier: HeaderCarrier): Future[Option[A]] = {
          Future.successful(Some(MatchingPerson("exNino", Some("exFirst"), Some("exLast"), Some("exDob"))).asInstanceOf[Some[A]])
        }
      }
      service.searchClient("", request).futureValue shouldBe Some(MatchingPerson("exNino", Some("exFirst"), Some("exLast"), Some("exDob")))
    }

    "return none if not found" in new WithApplication(FakeApplication()) {
      val request = SearchRequest("unknown", Some("exFirst"), Some("exLast"), None)
      val service = new AgentConnector {
        override protected def httpPostF[A](uri: String, body: JsValue, headers: Map[String, String])(implicit m: Manifest[A], headerCarrier: HeaderCarrier): Future[Option[A]] = {
          Future.successful(None)
        }
      }
      service.searchClient("", request).futureValue shouldBe None
    }

  }
  "The add client service" should {
    "handle a 200 response from the microservice" in new WithApplication(FakeApplication()) {
      val request = Client("CS700100A", Some("123456789"), PreferredContact(true, Some(Contact("foo", "foo@foo.com", "1234"))))
      val service = new AgentConnector {
        override protected def httpPostSynchronous(uri: String, body: JsValue, headers: Map[String, String])(implicit hc: HeaderCarrier): Response = new ResponseStub("", 200)
      }
      service.saveOrUpdateClient("", request)
    }

    "throw an exception in case of errors" in {
      val request = Client("CS700100A", Some("123456789"), PreferredContact(true, Some(Contact("foo", "foo@foo.com", "1234"))))
      val service = new AgentConnector {
        override protected def httpPostSynchronous(uri: String, body: JsValue, headers: Map[String, String])(implicit hc: HeaderCarrier): Response = new ResponseStub("", 500)
      }

      intercept[MicroServiceException] {
        service.saveOrUpdateClient("", request)
      }

    }
  }
}
