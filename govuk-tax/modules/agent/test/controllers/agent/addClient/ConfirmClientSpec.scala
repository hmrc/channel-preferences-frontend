package controllers.agent.addClient

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.scalatest.BeforeAndAfter
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import uk.gov.hmrc.common.microservice.agent.{MatchingPerson, AgentMicroService}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import scala.util.Success
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import play.api.test.{FakeApplication, WithApplication, FakeRequest}
import ConfirmClientController.FieldIds._
import SearchClientController.KeyStoreKeys._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.jsoup.Jsoup

class ConfirmClientSpec extends BaseSpec with MockitoSugar with BeforeAndAfter {

  def keyStore: KeyStoreMicroService = controller.keyStoreMicroService
  def agentService: AgentMicroService = controller.agentMicroService
  var controller: ConfirmClientController = _

  val id = "wshakespeare"
  val authority = s"/auth/oid/$id"
  val uri = "/personal/paye/blah"
  val payeRoot = PayeRoot("CE927349E", 1, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map(), Map())
  val user = User(id, null, RegimeRoots(Some(Success(payeRoot)), None, None, None, None), None, None)

  before {
    controller = new ConfirmClientController with MockMicroServicesForTests
  }

  "The confirm client page" should {
    "show an error if the user does not accept the client and not call the agent service"  in new WithApplication(FakeApplication()) {
      when(keyStore.getEntry[MatchingPerson](keystoreId(id), serviceSourceKey, clientSearchObjectKey)).thenReturn(Some(MatchingPerson("exnino", Some("exFirst"), Some("exLast"), Some("1990-01-01"))))
      val result = controller.confirmAction(user)(FakeRequest().withFormUrlEncodedBody(
        (correctClient, ""),
        (authorised, "true"),
        (internalClientRef, "1234")))
      status(result) should be (400)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(s".error #${correctClient}") should not be ('empty)
      doc.select(s".error #${authorised}") should be ('empty)
      doc.select(s".error #${internalClientRef}") should be ('empty)

      verifyZeroInteractions(agentService)
    }

    "show an error if the user does not confirm that they are authorised and not call the agent service"  in new WithApplication(FakeApplication()) {
      when(keyStore.getEntry[MatchingPerson](keystoreId(id), serviceSourceKey, clientSearchObjectKey)).thenReturn(Some(MatchingPerson("exnino", Some("exFirst"), Some("exLast"), Some("1990-01-01"))))
      val result = controller.confirmAction(user)(FakeRequest().withFormUrlEncodedBody(
        (correctClient, "true"),
        (authorised, ""),
        (internalClientRef, "1234")))
      status(result) should be (400)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(s".error #${correctClient}") should be ('empty)
      doc.select(s".error #${authorised}") should not be ('empty)
      doc.select(s".error #${internalClientRef}") should be ('empty)

      verifyZeroInteractions(agentService)
    }

    "redirect the user to the search client page if they do not have a stored search result"  in new WithApplication(FakeApplication()) {
      when(keyStore.getEntry[MatchingPerson](keystoreId(id), serviceSourceKey, clientSearchObjectKey)).thenReturn(None)
      val result = controller.confirmAction(user)(FakeRequest().withFormUrlEncodedBody(
        (correctClient, "true"),
        (authorised, "true"),
        (internalClientRef, "1234")))

      status(result) should be (303)
      redirectLocation(result) should contain (controllers.agent.addClient.routes.SearchClientController.start().url)

      verifyZeroInteractions(agentService)
    }
  }
}
