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
import models.agent.addClient.{ConfirmClient, ClientSearch}
import uk.gov.hmrc.common.microservice.agent._
import controllers.agent.addClient.SearchClientController.KeyStoreKeys._
import controllers.agent.addClient.PreferredClientController.FieldIds
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import models.agent.addClient.PotentialClient
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import play.api.test.FakeApplication
import service.agent.AgentMicroService
import concurrent.Future

class PreferredContactSpec extends BaseSpec with MockitoSugar with BeforeAndAfter {

  val id = "wshakespeare"
  val instanceId = "exampleInstanceId"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"
  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map(), Map())
  val user = User(
    userId = id,
    userAuthority = null,
    regimes =
      RegimeRoots(
        paye = Some(payeRoot),
        agent = Some(AgentRoot("SomeUAR", Map.empty, Map("search" -> "/agent/search", "addClient" -> "agent/addClient")))),
    nameFromGovernmentGateway = None,
    decryptedToken = None)

  val agentMicroService = mock[AgentMicroService]
  val keyStoreMicroService = mock[KeyStoreMicroService]
  //TODO: delete this and just use the above mock
  val confirmKeyStoreMicroService = mock[KeyStoreMicroService]

  val contactController: PreferredContactController = new PreferredContactController(keyStoreMicroService, null)(agentMicroService, null)

  val confirmController: ConfirmClientController = new ConfirmClientController(confirmKeyStoreMicroService, null)(null)

  before {
    reset(keyStoreMicroService, confirmKeyStoreMicroService, agentMicroService)
  }

  "When navigating to the preferred contact controller via the add action controller the controller" should {
    "return a 303 when there is no session in play" in new WithApplication(FakeApplication()) {
      val result = executeConfirmActionPostWithValues("true", "true", "FOO", instanceId)
      status(result) shouldBe 303
      redirectLocation(result) should contain(controllers.agent.addClient.routes.SearchClientController.restart().url)
    }

    "have the default radio button selected" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("AB123456C", Some("Foo"), Some("Bar"), None)
      val confirmation = Some(ConfirmClient(true, true, Some("reference")))
      when(confirmKeyStoreMicroService.getEntry[PotentialClient](keystoreId(id, instanceId), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), confirmation, None)))

      val result = executeConfirmActionPostWithValues("true", "true", "FOO", instanceId)
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      val elements = doc.select("input[checked]")
      elements.size should be(1)
      elements.get(0).getElementsByAttribute("value") is ("me")
    }

    "have the passed in instanceId in the page" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("AB123456C", Some("Foo"), Some("Bar"), None)
      val confirmation = Some(ConfirmClient(true, true, Some("reference")))
      when(confirmKeyStoreMicroService.getEntry[PotentialClient](keystoreId(id, instanceId), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), confirmation, None)))

      val result = executeConfirmActionPostWithValues("true", "true", "FOO", instanceId)
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(s"input#${FieldIds.instanceId}").attr("value") should equal(instanceId)
    }

    "not show any errors" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("AB123456C", Some("Foo"), Some("Bar"), None)
      val confirmation = Some(ConfirmClient(true, true, Some("reference")))
      when(confirmKeyStoreMicroService.getEntry[PotentialClient](keystoreId(id, instanceId), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), confirmation, None)))

      val result = executeConfirmActionPostWithValues("true", "true", "FOO", instanceId)
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error") should be('empty)
    }

    def executeConfirmActionPostWithValues(correctClient: String, authorised: String, internalClientReference: String, instanceId: String) = {
      val request = FakeRequest().withFormUrlEncodedBody(
        (ConfirmClientController.FieldIds.correctClient, correctClient),
        (ConfirmClientController.FieldIds.authorised, authorised),
        (ConfirmClientController.FieldIds.internalClientRef, internalClientReference),
        (ConfirmClientController.FieldIds.instanceId, instanceId))
      Future.successful(confirmController.confirmAction(user)(request))
    }
  }

  "When hitting the preferred contact controller the result" should {

    "return a 303 when there is no session in play" in new WithApplication(FakeApplication()) {
      val result = executePreferredContactActionPostWithValues(FieldIds.other, "", "123456", "v@v.com", instanceId)
      status(result) shouldBe 303
      redirectLocation(result) should contain(controllers.agent.addClient.routes.SearchClientController.start().url)
    }

    "have the other radio button selected when entering invalid data via the preferredContactAction" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("AB123456C", Some("Foo"), Some("Bar"), None)
      val confirmation = Some(ConfirmClient(true, true, Some("reference")))
      when(keyStoreMicroService.getEntry[PotentialClient](keystoreId(id, instanceId), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), confirmation, None)))
      val result = executePreferredContactActionPostWithValues(FieldIds.other, "", "123456", "v@v.com", instanceId)
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      val elements = doc.select("input[checked]")
      elements.size should be(1)
      elements.get(0).getElementsByAttribute("value") is (FieldIds.other)
    }

    "fail when no phone number is provided provided" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("AB123456C", Some("Foo"), Some("Bar"), None)
      val confirmation = Some(ConfirmClient(true, true, Some("reference")))
      when(keyStoreMicroService.getEntry[PotentialClient](keystoreId(id, instanceId), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), confirmation, None)))
      val result = executePreferredContactActionPostWithValues(FieldIds.other, "firstName", "", "email@email.com", instanceId)
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error #contactPhone") should not be 'empty
    }

    "fail when an invalid phone number is provided provided" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("AB123456C", Some("Foo"), Some("Bar"), None)
      val confirmation = Some(ConfirmClient(true, true, Some("reference")))
      when(keyStoreMicroService.getEntry[PotentialClient](keystoreId(id, instanceId), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), confirmation, None)))
      val result = executePreferredContactActionPostWithValues(FieldIds.other, "firstName", "aefwefw", "email@email.com", instanceId)
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error #contactPhone") should not be 'empty
    }

    "pass when suppling other contact and all data is correctly provided" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("AB123456C", Some("Foo"), Some("Bar"), None)
      val confirmation = Some(ConfirmClient(true, true, Some("reference")))
      when(keyStoreMicroService.getEntry[PotentialClient](keystoreId(id, instanceId), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), confirmation, None)))
      val result = executePreferredContactActionPostWithValues(FieldIds.other, "firstName", "1123", "email@email.com", instanceId)
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("body").text() should include("A client has been successfully added")
    }

    "pass when user is preferred contact and all data is correctly provided" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("AB123456C", Some("Foo"), Some("Bar"), None)
      val confirmation = Some(ConfirmClient(true, true, Some("reference")))
      when(keyStoreMicroService.getEntry[PotentialClient](keystoreId(id, instanceId), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), confirmation, None)))
      val result = executePreferredContactActionPostWithValues(FieldIds.me, "firstName", "lastName", "email@email.com", instanceId)
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("body").text() should include("A client has been successfully added")
    }

    "pass when not us is selected and all data is correctly provided" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("AB123456C", Some("Foo"), Some("Bar"), None)
      val confirmation = Some(ConfirmClient(true, true, Some("reference")))
      when(keyStoreMicroService.getEntry[PotentialClient](keystoreId(id, instanceId), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), confirmation, None)))
      val result = executePreferredContactActionPostWithValues(FieldIds.notUs, "", "", "", instanceId)
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("body").text() should include("A client has been successfully added")
    }

    def executePreferredContactActionPostWithValues(poc: String, name: String, phone: String, email: String, instanceId: String) = {
      val request = FakeRequest().withFormUrlEncodedBody(
        (FieldIds.pointOfContact, poc),
        (FieldIds.contactName, name),
        (FieldIds.contactPhone, phone),
        (FieldIds.contactEmail, email),
        (FieldIds.instanceId, instanceId)
      )
      Future.successful(contactController.preferredContactAction(user)(request))
    }
  }

  "When using the validation rules i expect that they" should {
    "fail an empty email" in {
      SearchClientController.Validation.validateEmail(Some("")) should be(false)
    }
    "fail a clearly invalid email email" in {
      SearchClientController.Validation.validateEmail(Some("erfgjerf8wefd")) should be(false)
    }
    "fail an email with two @ symbols" in {
      SearchClientController.Validation.validateEmail(Some("erfgj@erf8@wefd")) should be(false)
    }
    "pass a valid email" in {
      SearchClientController.Validation.validateEmail(Some("foo@foomail.com")) should be(true)
    }
  }


}
