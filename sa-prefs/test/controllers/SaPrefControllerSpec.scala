package controllers

import org.scalatest.{ ShouldMatchers, WordSpec }
import play.api.test.{ FakeApplication, WithApplication }
import play.api.test.FakeRequest
import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers._
import org.mockito.Mockito._
import uk.gov.hmrc.{ SaMicroService, MicroServiceException }
import org.joda.time.{ DateTimeZone, DateTime }
import org.apache.commons.codec.binary.Base64
import java.net.URLEncoder

class SaPrefControllerSpec extends WordSpec with ShouldMatchers with MockitoSugar {

  import play.api.test.Helpers._

  val validUtr = "1234567"
  lazy val validToken = URLEncoder.encode(SsoPayloadEncryptor.encrypt(s"$validUtr:${DateTime.now(DateTimeZone.UTC).getMillis}"), "UTF-8")
  val validReturnUrl = URLEncoder.encode("http://localhost:8080/portal", "UTF-8")

  def createController = new SaPrefsController {
    override lazy val saMicroService = mock[SaMicroService]
  }

  "Preferences pages" should {
    "render an email input field" in new WithApplication(FakeApplication()) {

      val controller = createController

      val page = controller.index(validToken, validReturnUrl)(FakeRequest())
      contentAsString(page) should include("email")

    }

    "include a link to keep mail preference" in new WithApplication(FakeApplication()) {
      val controller = createController

      val page = controller.index(validToken, validReturnUrl)(FakeRequest())
      (pending)
    }

  }

  "A post to set preferences" should {
    "redirect to a confirmation page" in new WithApplication(FakeApplication()) {
      val controller = createController

      val page = controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email", "foo@bar.com")))

      status(page) shouldBe 303
      header("Location", page).get should include(s"/sa/print-preferences-saved?return_url=$validReturnUrl")
    }

    "show an error if the email is invalid" in new WithApplication(FakeApplication()) {
      val controller = createController

      val page = controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email", "invalid-email")))

      status(page) shouldBe 400
      contentAsString(page) should include("Please provide a valid email address</span>\n    \n</div>")
      verifyZeroInteractions(controller.saMicroService)
    }

    "show an error if the email is not set" in new WithApplication(FakeApplication()) {
      val controller = createController

      val page = controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email", "")))

      status(page) shouldBe 400
      contentAsString(page) should include("Please provide a valid email address</span>\n    \n</div>")
      verify(controller.saMicroService, times(0)).savePreferences(any[String], any[Boolean], any[Option[String]])
    }

    "save the user preferences" in new WithApplication(FakeApplication()) {
      val controller = createController

      controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email", "foo@bar.com")))
      verify(controller.saMicroService, times(1)).savePreferences(validUtr, true, Some("foo@bar.com"))
    }

  }
}