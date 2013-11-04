package controllers.common.actions

import play.api.mvc.{ Call, Controller }
import uk.gov.hmrc.common.microservice.domain.User
import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.common.BaseSpec
import concurrent.Future
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector

sealed class MultiFormSpecController(override val keyStoreConnector: KeyStoreConnector)
  extends Controller 
  with MultiFormWrapper 
  with MockitoSugar {

  val call1: Call = mock[Call]
  val call2: Call = mock[Call]
  val call3: Call = mock[Call]

  def multiformConfiguration(user: User) = {
    MultiFormConfiguration(
      "userId",
      "source",
      List(
        MultiFormStep("step1", call1),
        MultiFormStep("step2", call2),
        MultiFormStep("step3", call3)
      ),
      "step3",
      MultiFormStep("step1", call1)
    )
  }

  def multiformConfigurationHomePage(user: User) = {
    MultiFormConfiguration(
      "userId",
      "source",
      List(
        MultiFormStep("step1", call1),
        MultiFormStep("step2", call2),
        MultiFormStep("step3", call3)
      ),
      "step1",
      MultiFormStep("step1", call1)
    )
  }

  def testJumpAhead() =
    MultiFormAction(user => multiformConfiguration(user)) {
      user =>
        request =>
          Ok("You are in step 3!")
    }

  def testHomePage() =
    MultiFormAction(user => multiformConfigurationHomePage(user)) {
      user =>
        request =>
          Ok("You are in home page")
    }
}

class MultiFormWrapperSpec extends BaseSpec with MockitoSugar {

  "MultiformWrapper" should {

    "redirect to step1 when keystore does not exist and user attempts to go to step3" in new WithApplication(FakeApplication()) {
      val user: User = mock[User]
      val keyStoreConnector = mock[KeyStoreConnector]
      val controller = new MultiFormSpecController(keyStoreConnector)
      
      when(keyStoreConnector.getDataKeys("userId", "source")).thenReturn(None)
      when(controller.call1.url).thenReturn("/step1")

      val result = Future.successful(controller.testJumpAhead()(user)(FakeRequest()))
      status(result) should be(303)
      headers(result).get("Location") should contain("/step1")
    }

    "allow access to step1 when user tries to access it for the first time and key store does not exist" in new WithApplication(FakeApplication()) {
      val user: User = mock[User]
      val keyStoreConnector = mock[KeyStoreConnector]
      val controller = new MultiFormSpecController(keyStoreConnector)

      when(keyStoreConnector.getDataKeys("userId", "source")).thenReturn(None)

      val result = Future.successful(controller.testHomePage()(user)(FakeRequest()))
      status(result) should be(200)
      contentAsString(result) should be("You are in home page")
    }

    "redirect to step1 when keystore exists but the data keys set is empty" in new WithApplication(FakeApplication()) {

      val user: User = mock[User]
      val keyStoreConnector = mock[KeyStoreConnector]
      val controller = new MultiFormSpecController(keyStoreConnector)

      when(keyStoreConnector.getDataKeys("userId", "source")).thenReturn(Some(Set.empty[String]))
      when(controller.call1.url).thenReturn("/step1")

      val result = Future.successful(controller.testJumpAhead()(user)(FakeRequest()))
      status(result) should be(303)
      headers(result).get("Location") should contain("/step1")
    }

    "go to step3 when keystore exists and the previous steps were completed" in new WithApplication(FakeApplication()) {

      val user: User = mock[User]
      val keyStoreConnector = mock[KeyStoreConnector]
      val controller = new MultiFormSpecController(keyStoreConnector)

      when(keyStoreConnector.getDataKeys("userId", "source")).thenReturn(Some(Set("step1", "step2")))

      val result = Future.successful(controller.testJumpAhead()(user)(FakeRequest()))
      status(result) should be(200)
      contentAsString(result) should be("You are in step 3!")
    }

    "redirect to step2 when trying to go to step3 and in keystore only step1 was completed" in new WithApplication(FakeApplication()) {

      val user: User = mock[User]
      val keyStoreConnector = mock[KeyStoreConnector]
      val controller = new MultiFormSpecController(keyStoreConnector)

      when(keyStoreConnector.getDataKeys("userId", "source")).thenReturn(Some(Set("step1")))
      when(controller.call2.url).thenReturn("/step2")

      val result = Future.successful(controller.testJumpAhead()(user)(FakeRequest()))
      status(result) should be(303)
      headers(result).get("Location") should contain("/step2")
    }

  }

}
