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
import models.agent.addClient.{ConfirmClient, PotentialClient, ClientSearch}
import scala.util.Success
import uk.gov.hmrc.common.microservice.agent.{MatchingPerson, SearchRequest, AgentMicroService}
import SearchClientController.KeyStoreKeys
import SearchClientController.FieldIds
import uk.gov.hmrc.common.microservice.domain.User
import scala.util.Success
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import scala.Some
import uk.gov.hmrc.common.microservice.agent.AgentMicroService
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import controllers.agent.addClient.SearchClientController.KeyStoreKeys._
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.agent.MatchingPerson
import scala.Some
import uk.gov.hmrc.common.microservice.domain.User
import scala.util.Success
import play.api.test.FakeApplication
import PreferredClientController.FieldIds._

class PreferredContactSpec extends BaseSpec with MockitoSugar with BeforeAndAfter {

  def keyStore: KeyStoreMicroService = controller.keyStoreMicroService
  def agentService: AgentMicroService = controller.agentMicroService
  var controller: PreferredContactController = _
  var confirmController: ConfirmClientController = _

  val id = "wshakespeare"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"
  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map(), Map())
  val user = User(id, null, RegimeRoots(Some(Success(payeRoot)), None, None, None, None), None, None)

  before {
    controller = new PreferredContactController with MockMicroServicesForTests
    confirmController = new ConfirmClientController with MockMicroServicesForTests
  }

  def executeConfirmActionPostWithValues(correctClient: String, authorised: String, internalClientReference: String, instanceId: String) = {
    val request = FakeRequest().withFormUrlEncodedBody(
      (ConfirmClientController.FieldIds.correctClient, correctClient),
      (ConfirmClientController.FieldIds.authorised, authorised),
      (ConfirmClientController.FieldIds.internalClientRef, internalClientReference),
      (ConfirmClientController.FieldIds.instanceId, instanceId))
    confirmController.confirmAction(user)(request)
  }

  def executePreferredContactActionPostWithValues(poc: String, name: String, phone: String, email: String) = {
    val request = FakeRequest().withFormUrlEncodedBody(
      (pointOfContact, poc),
      (contactName, name),
      (contactPhone, phone),
      (contactEmail, email)
    )
    controller.preferredContactAction(user)(request)
  }

  "When navigating to the preferred contact controller via the add action controller the controller" should {
    "return a 303 when there is no session in play" in new WithApplication(FakeApplication()) {
      val result = executeConfirmActionPostWithValues("true", "true", "FOO", "instID")
      status(result) shouldBe 303
      redirectLocation(result) should contain (controllers.agent.addClient.routes.SearchClientController.start().url)
    }
  }

  "When hitting the preferred contact controller the result" should {

    "return a 303 when there is no session in play" in new WithApplication(FakeApplication()) {
      val result = executePreferredContactActionPostWithValues(other, "", "123456", "v@v.com")
      status(result) shouldBe 303
      redirectLocation(result) should contain (controllers.agent.addClient.routes.SearchClientController.start().url)
    }

    "have the default radio button selected when entering via the add action controller" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("AB123456C", Some("Foo"), Some("Bar"), None)
      val confirmation = Some(ConfirmClient(true, true, Some("reference")))
      when(confirmController.keyStoreMicroService.getEntry[PotentialClient](keystoreId(id), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), confirmation, None)))

      val result = executeConfirmActionPostWithValues("true", "true", "FOO", "instID")
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      val elements = doc.select("input[checked]")
      elements.size should be (1)
      elements.get(0).getElementsByAttribute("value") is ("me")
    }

    "have the other radio button selected when entering invalid data via the preferredContactAction" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("AB123456C", Some("Foo"), Some("Bar"), None)
      val confirmation = Some(ConfirmClient(true, true, Some("reference")))
      when(keyStore.getEntry[PotentialClient](keystoreId(id), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), confirmation, None)))
      val result = executePreferredContactActionPostWithValues(other, "", "123456", "v@v.com")
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      val elements = doc.select("input[checked]")
      elements.size should be (1)
      elements.get(0).getElementsByAttribute("value") is (other)
    }

    "fail when no phone number is provided provided" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("AB123456C", Some("Foo"), Some("Bar"), None)
      val confirmation = Some(ConfirmClient(true, true, Some("reference")))
      when(keyStore.getEntry[PotentialClient](keystoreId(id), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), confirmation, None)))
      val result = executePreferredContactActionPostWithValues(other, "firstName", "", "email@email.com")
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error #contactPhone") should not be 'empty
    }

    "fail when an invalid phone number is provided provided" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("AB123456C", Some("Foo"), Some("Bar"), None)
      val confirmation = Some(ConfirmClient(true, true, Some("reference")))
      when(keyStore.getEntry[PotentialClient](keystoreId(id), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), confirmation, None)))
      val result = executePreferredContactActionPostWithValues(other, "firstName", "aefwefw", "email@email.com")
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error #contactPhone") should not be 'empty
    }

    "pass when suppling other contact and all data is correctly provided" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("AB123456C", Some("Foo"), Some("Bar"), None)
      val confirmation = Some(ConfirmClient(true, true, Some("reference")))
      when(keyStore.getEntry[PotentialClient](keystoreId(id), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), confirmation, None)))
      val result = executePreferredContactActionPostWithValues(other, "firstName", "1123", "email@email.com")
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("body").text() should include ("A client has been successfully added")
    }

    "pass when user is preferred contact and all data is correctly provided" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("AB123456C", Some("Foo"), Some("Bar"), None)
      val confirmation = Some(ConfirmClient(true, true, Some("reference")))
      when(keyStore.getEntry[PotentialClient](keystoreId(id), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), confirmation, None)))
      val result = executePreferredContactActionPostWithValues(me, "firstName", "lastName", "email@email.com")
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("body").text() should include ("A client has been successfully added")
    }

    "pass when not us is selected and all data is correctly provided" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("AB123456C", Some("Foo"), Some("Bar"), None)
      val confirmation = Some(ConfirmClient(true, true, Some("reference")))
      when(keyStore.getEntry[PotentialClient](keystoreId(id), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), confirmation, None)))
      val result = executePreferredContactActionPostWithValues(notUs, "", "", "")
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
