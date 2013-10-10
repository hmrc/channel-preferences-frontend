package controllers.agent.addClient

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import uk.gov.hmrc.common.microservice.agent.AgentMicroService
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import controllers.agent.addClient.SearchClientController.KeyStoreKeys._
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.agent.MatchingPerson
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import scala.util.Success
import play.api.test.FakeApplication
import controllers.agent.addClient.ConfirmClientController

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
      (ConfirmClientController.FieldIds.correctClient, correctClient),
      (ConfirmClientController.FieldIds.authorised, authorised),
      (ConfirmClientController.FieldIds.internalClientRef, internalClientReference))
    controller.confirmAction(user)(request)
  }

  def executePreferredContactActionPostWithValues(poc: String, name: String, phone: String, email: String) = {
    val request = FakeRequest().withFormUrlEncodedBody(
      (ConfirmClientController.pointOfContact, poc),
      (ConfirmClientController.contactName, name),
      (ConfirmClientController.contactPhone, phone),
      (ConfirmClientController.contactEmail, email)
    )
    controller.preferredContactAction(user)(request)
  }

  "When navigating to the preferred contact controller via the add action controller the controller" should {
    "return a 303 when there is no session in play" in new WithApplication(FakeApplication()) {
      val result = executeAddActionPostWithValues("true", "true", "FOO")
      status(result) shouldBe 303
      redirectLocation(result) should contain (controllers.agent.addClient.routes.SearchClientController.start().url)
    }
  }

  "When hitting the preferred contact controller the result" should {

    "return a 303 when there is no session in play" in new WithApplication(FakeApplication()) {
      val result = executePreferredContactActionPostWithValues(ConfirmClientController.other, "", "123456", "v@v.com")
      status(result) shouldBe 303
      redirectLocation(result) should contain (controllers.agent.addClient.routes.SearchClientController.start().url)
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
      val result = executePreferredContactActionPostWithValues(ConfirmClientController.other, "", "123456", "v@v.com")
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      val elements = doc.select("input[checked]")
      elements.size should be (1)
      elements.get(0).getElementsByAttribute("value") is (ConfirmClientController.other)
    }

    "pass when suppling other contact and all data is correctly provided" in new WithApplication(FakeApplication()) {
      val person = Some(MatchingPerson("AB123456C", Some("Foo"), Some("Bar"), None))
      when(keyStore.getEntry[MatchingPerson](keystoreId(user.oid), serviceSourceKey, clientSearchObjectKey)).thenReturn(person)
      val result = executePreferredContactActionPostWithValues(ConfirmClientController.other, "firstName", "lastName", "email@email.com")
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("body").text() should include ("A client has been successfully added")
    }

    "pass when user is preferred contact and all data is correctly provided" in new WithApplication(FakeApplication()) {
      val person = Some(MatchingPerson("AB123456C", Some("Foo"), Some("Bar"), None))
      when(keyStore.getEntry[MatchingPerson](keystoreId(user.oid), serviceSourceKey, clientSearchObjectKey)).thenReturn(person)
      val result = executePreferredContactActionPostWithValues(ConfirmClientController.me, "firstName", "lastName", "email@email.com")
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("body").text() should include ("A client has been successfully added")
    }

    "pass when not us is selected and all data is correctly provided" in new WithApplication(FakeApplication()) {
      val person = Some(MatchingPerson("AB123456C", Some("Foo"), Some("Bar"), None))
      when(keyStore.getEntry[MatchingPerson](keystoreId(user.oid), serviceSourceKey, clientSearchObjectKey)).thenReturn(person)
      val result = executePreferredContactActionPostWithValues(ConfirmClientController.notUs, "", "", "")
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("body").text() should include ("A client has been successfully added")
    }
  }

  "When using the validation rules i expect that they" should {
    "fail an empty email" in {
      SearchClientController.Validation.validateEmail(Some("")) should be (false)
    }
    "fail a clearly invalid email email" in {
      SearchClientController.Validation.validateEmail(Some("erfgjerf8wefd")) should be (false)
    }
    "fail an email with two @ symbols" in {
      SearchClientController.Validation.validateEmail(Some("erfgj@erf8@wefd")) should be (false)
    }
    "pass a valid email" in {
      SearchClientController.Validation.validateEmail(Some("foo@foomail.com")) should be (true)
    }
  }


}
