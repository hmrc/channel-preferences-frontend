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
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import controllers.agent.addClient.SearchClientController.KeyStoreKeys._
import uk.gov.hmrc.common.microservice.domain.User
import scala.util.Success
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.agent.MatchingPerson
import scala.Some
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.agent.MatchingPerson
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import scala.util.Success
import play.api.test.FakeApplication

class PreferredContactSpec extends BaseSpec with MockitoSugar with BeforeAndAfter {

  var keyStore: KeyStoreMicroService = _
  var agentService: AgentMicroService = _
  var controller: ConfirmClientController = _

  val id = "wshakespeare"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"
  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map(), Map())
  val user = User(id, null, RegimeRoots(Some(Success(payeRoot)), None, None, None, None), None, None)

  before {
    agentService = mock[AgentMicroService]
    controller = new ConfirmClientController with MockMicroServicesForTests {
      override implicit lazy val agentMicroService: AgentMicroService = agentService
    }
    keyStore = controller.keyStoreMicroService
  }

  def executeAddActionPostWithValues(correctClient: String, authorised: String, internalClientReference: String) = {
    val request = FakeRequest().withFormUrlEncodedBody(
      ("correctClient", correctClient),
      ("authorised", authorised),
      ("internalClientReference", internalClientReference))
    controller.addAction(user)(request)
  }

  def executePreferredContactActionPostWithValues(poc: String, name: String, phone: String, email: String) = {
    val request = FakeRequest().withFormUrlEncodedBody(
      ("pointOfContact", poc),
      ("contactName", name),
      ("contactPhone", phone),
      ("contactEmail", email)
    )
    controller.preferredContactAction(user)(request)
  }

  "Given the system has loaded the preferred contact page" should {

    "return a 400 when there is no session in play" in new WithApplication(FakeApplication()) {
      val result = executeAddActionPostWithValues("true", "true", "FOO")
      status(result) shouldBe 400
    }

    "have the default radio button selected when entering via the add action controller" in new WithApplication(FakeApplication()) {
      val person = Some(MatchingPerson("AB123456C", Some("Foo"), Some("Bar"), None))
      when(keyStore.getEntry[MatchingPerson](keystoreId(user.oid), serviceSourceKey, clientSearchObjectKey)).thenReturn(person)
      val result = executeAddActionPostWithValues("true", "true", "FOO")
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      val elements = doc.select("input[checked]")
      elements.size should be (1)
      elements.get(0).getElementsByAttribute("value") is ("me")
    }

    "have the other radio button selected when entering invalid data via the preferredContactAction" in new WithApplication(FakeApplication()) {
      val person = Some(MatchingPerson("AB123456C", Some("Foo"), Some("Bar"), None))
      when(keyStore.getEntry[MatchingPerson](keystoreId(user.oid), serviceSourceKey, clientSearchObjectKey)).thenReturn(person)
      val result = executePreferredContactActionPostWithValues("other", "", "123456", "v@v.com")
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      val elements = doc.select("input[checked]")
      elements.size should be (1)
      elements.get(0).getElementsByAttribute("value") is ("other")
    }


  }

}
