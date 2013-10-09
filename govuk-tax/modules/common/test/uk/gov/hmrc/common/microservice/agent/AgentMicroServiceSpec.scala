package uk.gov.hmrc.common.microservice.agent

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.JsValue
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeApplication, WithApplication}

class AgentMicroServiceSpec extends WordSpec with Matchers with MockitoSugar {

  "The agent micro service" should {
    "return the result when found"  in new WithApplication(FakeApplication())  {
      val request = SearchRequest("exNino", Some("exFirst"), Some("exLast"), None)
      val service = new AgentMicroService {
        override protected def httpPost[A](uri: String, body: JsValue, headers: Map[String, String])(implicit m: Manifest[A]): Option[A] = {
          Some(MatchingPerson("exNino", Some("exFirst"), Some("exLast"), Some("exDob"))).asInstanceOf[Some[A]]
        }
      }
      service.searchClient(request) shouldBe Some(MatchingPerson("exNino", Some("exFirst"), Some("exLast"), Some("exDob")))
    }

    "return none if not found"  in new WithApplication(FakeApplication()) {
      val request = SearchRequest("unknown", Some("exFirst"), Some("exLast"), None)
      val service = new AgentMicroService {
        override protected def httpPost[A](uri: String, body: JsValue, headers: Map[String, String])(implicit m: Manifest[A]): Option[A] = {
          None
        }
      }
      service.searchClient(request) shouldBe None
    }
  }
}
