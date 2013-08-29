package controllers.common.actions

import play.api.mvc.Controller
import uk.gov.hmrc.microservice.MockMicroServicesForTests
import uk.gov.hmrc.microservice.domain.User
import controllers.common.routes
import play.api.test.{ FakeRequest, FakeApplication, WithApplication }
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._

class MultiFormController extends Controller with MultiFormWrapper with MockMicroServicesForTests {

  def multiformConfiguration(user: User) = {
    MultiFormConfiguration(
      "userId", "source", List("step1", "step2", "step3"), "step3", routes.LoginController.login
    )
  }

  def test() =
    MultiFormAction(user => multiformConfiguration(user)) {
      user =>
        request =>
          Ok("You are in step 3!")
    }
}

class MultiFormWrapperSpec extends WordSpec with MustMatchers with MockitoSugar {

  "MultiformWrapper" should {

    "go to the login controller when keystore does not exist" in new WithApplication(FakeApplication()) {

      val user: User = mock[User]
      val controller = new MultiFormController
      when(controller.keyStoreMicroService.getDataKeys("userId", "source")).thenReturn(None)

      val result = controller.test()(user)(FakeRequest())
      status(result) must be(303)
      headers(result)("Location") must be("/login")
    }

    "go to the login controller when keystore exists but the data keys set is empty" in new WithApplication(FakeApplication()) {

      val user: User = mock[User]
      val controller = new MultiFormController
      when(controller.keyStoreMicroService.getDataKeys("userId", "source")).thenReturn(Some(Set.empty[String]))

      val result = controller.test()(user)(FakeRequest())
      status(result) must be(303)
      headers(result)("Location") must be("/login")
    }

    "go to the home controller when keystore exists and the previous steps were completed" in new WithApplication(FakeApplication()) {

      val user: User = mock[User]
      val controller = new MultiFormController
      when(controller.keyStoreMicroService.getDataKeys("userId", "source")).thenReturn(Some(Set("step1", "step2")))

      val result = controller.test()(user)(FakeRequest())
      status(result) must be(200)
      contentAsString(result) must be("You are in step 3!")
    }

    "go to the login controller when keystore exists but the previous steps were not completed" in new WithApplication(FakeApplication()) {

      val user: User = mock[User]
      val controller = new MultiFormController
      when(controller.keyStoreMicroService.getDataKeys("userId", "source")).thenReturn(Some(Set("step1")))

      val result = controller.test()(user)(FakeRequest())
      status(result) must be(303)
      headers(result)("Location") must be("/login")
    }

  }

}
