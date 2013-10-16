package service.agent

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.JsValue
import org.scalatest.mock.MockitoSugar
import play.api.test.WithApplication
import controllers.service.ResponseStub
import org.slf4j.MDC
import scala.collection.JavaConversions._
import models.agent._
import models.agent.Client
import play.api.libs.ws.Response
import models.agent.PreferredContact
import models.agent.SearchRequest
import models.agent.MatchingPerson
import play.api.test.FakeApplication
import scala.Some
import uk.gov.hmrc.microservice.MicroServiceException

class AgentMicroServiceSpec extends WordSpec with Matchers with MockitoSugar {

  "The agent micro search service" should {
    "return the result when found"  in new WithApplication(FakeApplication())  {
      val request = SearchRequest("exNino", Some("exFirst"), Some("exLast"), None)
      val service = new AgentMicroService {
        override protected def httpPost[A](uri: String, body: JsValue, headers: Map[String, String])(implicit m: Manifest[A]): Option[A] = {
          Some(MatchingPerson("exNino", Some("exFirst"), Some("exLast"), Some("exDob"))).asInstanceOf[Some[A]]
        }
      }
      service.searchClient("", request) shouldBe Some(MatchingPerson("exNino", Some("exFirst"), Some("exLast"), Some("exDob")))
    }

    "return none if not found"  in new WithApplication(FakeApplication()) {
      val request = SearchRequest("unknown", Some("exFirst"), Some("exLast"), None)
      val service = new AgentMicroService {
        override protected def httpPost[A](uri: String, body: JsValue, headers: Map[String, String])(implicit m: Manifest[A]): Option[A] = {
          None
        }
      }
      service.searchClient("", request) shouldBe None
    }

  }
  "The add client service" should {
    "handle a 200 response from the microservice"  in new WithApplication(FakeApplication())  {
      val request = Client("CS700100A", Some("123456789"), PreferredContact(true, Some(Contact("foo", "foo@foo.com", "1234"))))
      val service = new AgentMicroService {
        override protected def httpPostSynchronous(uri: String, body: JsValue, headers: Map[String, String]): Response = new ResponseStub("", 200)
      }
      service.saveOrUpdateClient("", request)
    }

    "throw an exception in case of errors" in {
      val request = Client("CS700100A", Some("123456789"), PreferredContact(true, Some(Contact("foo", "foo@foo.com", "1234"))))
      val service = new AgentMicroService {
        override protected def httpPostSynchronous(uri: String, body: JsValue, headers: Map[String, String]): Response = new ResponseStub("", 500)
      }

      MDC.setContextMap(Map.empty[String, String])

      intercept[MicroServiceException] {
        service.saveOrUpdateClient("", request)
      }

    }
  }
}
