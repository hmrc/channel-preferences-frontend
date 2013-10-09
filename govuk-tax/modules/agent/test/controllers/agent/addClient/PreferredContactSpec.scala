package controllers.agent.addClient

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import play.api.test.FakeApplication
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import org.joda.time.LocalDate
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.BeforeAndAfter
import models.agent.addClient.ClientSearch
import scala.util.Success
import uk.gov.hmrc.common.microservice.agent.{MatchingPerson, SearchRequest, AgentMicroService}
import SearchClientController.KeyStoreKeys
import SearchClientController.FieldIds
import uk.gov.hmrc.common.microservice.domain.User
import scala.util.Success
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import scala.Some

class PreferredContactSpec extends BaseSpec with MockitoSugar with BeforeAndAfter {

  var keyStore: KeyStoreMicroService = _
  var agentService: AgentMicroService = _
  var controller: SearchClientController = _

  val id = "wshakespeare"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"
  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map(), Map())
  val user = User(id, null, RegimeRoots(Some(Success(payeRoot)), None, None, None, None), None, None)

  before {
    keyStore = mock[KeyStoreMicroService]
    agentService = mock[AgentMicroService]
    controller = new SearchClientController(keyStore) {
      override implicit lazy val agentMicroService: AgentMicroService = agentService
    }
  }

  def executePostWithValues(correctClient: String, authorised: String, internalClientReference: String) = {
    val request = FakeRequest().withFormUrlEncodedBody(
      ("correctClient", correctClient),
      ("authorised", authorised),
      ("internalClientReference", internalClientReference))
    controller.addAction(user)(request)
  }

  "Given the system has loaded the preferred contact page" should {
    "return a 400 when there is no session in play" in {
      val result = executePostWithValues("true", "true", "FOO")
      status(result) shouldBe 400
    }
    "have the following default values" in {
      keyStore.addKeyStoreEntry()
      val result = executePostWithValues("true", "true", "FOO")
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
    }
  }

}
