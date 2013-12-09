package controllers.agent.addClient

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.scalatest.BeforeAndAfter
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import play.api.test.{FakeApplication, WithApplication, FakeRequest}
import ConfirmClientController.FieldIds
import SearchClientController.KeyStoreKeys._
import org.mockito.Mockito._
import org.jsoup.Jsoup
import models.agent.addClient.{ClientSearch, PotentialClient, ConfirmClient}
import org.joda.time.LocalDate
import service.agent.AgentConnector
import concurrent.Future
import org.mockito.Matchers
import controllers.common.actions.HeaderCarrier

class ConfirmClientSpec extends BaseSpec with MockitoSugar with BeforeAndAfter {

  val agentMicroService = mock[AgentConnector]
  val keyStoreConnector = mock[KeyStoreConnector]

  val controller: ConfirmClientController = new ConfirmClientController(keyStoreConnector, null)(null)

  val id = "wshakespeare"
  val instanceId = "exampleInstanceId"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"
  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map(), Map())
  val user = User(id, null, RegimeRoots(Some(payeRoot), None, None, None, None), None, None)

  before {
    reset(keyStoreConnector, agentMicroService)
  }

  "The confirm client page" should {
    "show an error if the user does not accept the client" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("exnino", Some("exFirst"), Some("exLast"), Some(new LocalDate(1990, 1, 1)))
      when(keyStoreConnector.getEntry[PotentialClient](actionId(instanceId), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), None, None)))
      val result = Future.successful(controller.confirmAction(user)(FakeRequest().withFormUrlEncodedBody(
        (FieldIds.correctClient, ""),
        (FieldIds.authorised, "true"),
        (FieldIds.internalClientRef, "1234"),
        (FieldIds.instanceId, instanceId))))
      status(result) should be(400)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(s".error #${FieldIds.correctClient}") should not be ('empty)
      doc.select(s".error #${FieldIds.authorised}") should be('empty)
      doc.select(s".error #${FieldIds.internalClientRef}") should be('empty)
    }

    "show an error if the user does not confirm that they are authorised" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("exnino", Some("exFirst"), Some("exLast"), Some(new LocalDate(1990, 1, 1)))
      when(keyStoreConnector.getEntry[PotentialClient](actionId(instanceId), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), None, None)))
      val result = Future.successful(controller.confirmAction(user)(FakeRequest().withFormUrlEncodedBody(
        (FieldIds.correctClient, "true"),
        (FieldIds.authorised, ""),
        (FieldIds.internalClientRef, "1234"),
        (FieldIds.instanceId, instanceId))))
      status(result) should be(400)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(s".error #${FieldIds.correctClient}") should be('empty)
      doc.select(s".error #${FieldIds.authorised}") should not be ('empty)
      doc.select(s".error #${FieldIds.internalClientRef}") should be('empty)
    }

    import Matchers._
    "redirect the user to the search client page if they do not have any potential client stored" in new WithApplication(FakeApplication()) {
      when(keyStoreConnector.getEntry[PotentialClient](anyString(), anyString(), anyString(), anyBoolean)(anyObject(), anyObject())).thenReturn(None)

      val result = controller.confirmAction(user)(FakeRequest().withFormUrlEncodedBody(
        (FieldIds.correctClient, "true"),
        (FieldIds.authorised, "true"),
        (FieldIds.internalClientRef, "1234")))

      status(result) should be(303)
      redirectLocation(result) should contain(controllers.agent.addClient.routes.SearchClientController.restart().url)
    }

    "redirect the user to the search client page if they do not have a search result in their potential client" in new WithApplication(FakeApplication()) {
      when(keyStoreConnector.getEntry[PotentialClient](anyString(), anyString(), anyString(), anyBoolean)(anyObject(), anyObject())).thenReturn(Some(PotentialClient(None, None, None)))
      val result = controller.confirmAction(user)(FakeRequest().withFormUrlEncodedBody(
        (FieldIds.correctClient, "true"),
        (FieldIds.authorised, "true"),
        (FieldIds.internalClientRef, "1234")))

      status(result) should be(303)
      redirectLocation(result) should contain(controllers.agent.addClient.routes.SearchClientController.restart().url)
    }

    "save the succesful acknoledgement to the keystore and show the prefered contact view" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("exnino", Some("exFirst"), Some("exLast"), Some(new LocalDate(1990, 1, 1)))
      when(keyStoreConnector.getEntry[PotentialClient](actionId(instanceId), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), None, None)))
      val result = Future.successful(controller.confirmAction(user)(FakeRequest().withFormUrlEncodedBody(
        (FieldIds.correctClient, "true"),
        (FieldIds.authorised, "true"),
        (FieldIds.internalClientRef, ""),
        (FieldIds.instanceId, instanceId))))
      status(result) should be(200)
      verify(keyStoreConnector).addKeyStoreEntry(actionId(instanceId), serviceSourceKey, addClientKey,
        PotentialClient(Some(clientSearch), Some(ConfirmClient(true, true, None)), None))
      contentAsString(result) should include("preferred point of contact")
    }

    "save the succesful acknoledgement and internal ref to the keystore and show the prefered contact view" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("exnino", Some("exFirst"), Some("exLast"), Some(new LocalDate(1990, 1, 1)))
      when(keyStoreConnector.getEntry[PotentialClient](actionId(instanceId), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), None, None)))
      val result = Future.successful(controller.confirmAction(user)(FakeRequest().withFormUrlEncodedBody(
        (FieldIds.correctClient, "true"),
        (FieldIds.authorised, "true"),
        (FieldIds.internalClientRef, "1234567"),
        (FieldIds.instanceId, instanceId))))
      status(result) should be(200)
      verify(keyStoreConnector).addKeyStoreEntry(actionId(instanceId), serviceSourceKey, addClientKey,
        PotentialClient(Some(clientSearch), Some(ConfirmClient(true, true, Some("1234567"))), None))
      contentAsString(result) should include("preferred point of contact")
    }

    "save the succesful acknoledgement and whitespace only internal ref to the keystore and show the prefered contact view" in new WithApplication(FakeApplication()) {
      val clientSearch = ClientSearch("exnino", Some("exFirst"), Some("exLast"), Some(new LocalDate(1990, 1, 1)))
      when(keyStoreConnector.getEntry[PotentialClient](actionId(instanceId), serviceSourceKey, addClientKey))
        .thenReturn(Some(PotentialClient(Some(clientSearch), None, None)))
      val result = Future.successful(controller.confirmAction(user)(FakeRequest().withFormUrlEncodedBody(
        (FieldIds.correctClient, "true"),
        (FieldIds.authorised, "true"),
        (FieldIds.internalClientRef, "      "),
        (FieldIds.instanceId, instanceId))))
      val s = contentAsString(result)
      status(result) should be(200)
      verify(keyStoreConnector).addKeyStoreEntry(actionId(instanceId), serviceSourceKey, addClientKey,
        PotentialClient(Some(clientSearch), Some(ConfirmClient(true, true, None)), None))
      contentAsString(result) should include("preferred point of contact")
    }
  }
}
