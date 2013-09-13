package controllers

import org.scalatest.{ ShouldMatchers, WordSpec }
import play.api.test.{ FakeApplication, WithApplication }
import play.api.test.FakeRequest
import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers._
import org.mockito.Mockito._
import uk.gov.hmrc.{ SaPreference, SaMicroService }
import org.joda.time.{ DateTimeZone, DateTime }
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
    "redirect to the portal when preferences already exist for a specific utr" in new WithApplication(FakeApplication()) {

      val controller = createController
      val preferencesAlreadyCreated = SaPreference(true, Some("test@test.com"))
      when(controller.saMicroService.getPreferences(validUtr)).thenReturn(Some(preferencesAlreadyCreated))

      val page = controller.index(validToken, validReturnUrl)(FakeRequest())
      status(page) shouldBe 303
      header("Location", page).get should include(validReturnUrl)
      verify(controller.saMicroService, times(1)).getPreferences(validUtr)

    }

    "render an email input field" in new WithApplication(FakeApplication()) {

      val controller = createController
      when(controller.saMicroService.getPreferences(validUtr)).thenReturn(None)

      val page = controller.index(validToken, validReturnUrl)(FakeRequest())
      contentAsString(page) should include("email")
      verify(controller.saMicroService, times(1)).getPreferences(validUtr)

    }

    "include a link to keep mail preference" in new WithApplication(FakeApplication()) {
      val controller = createController
      when(controller.saMicroService.getPreferences(validUtr)).thenReturn(None)

      val page = controller.index(validToken, validReturnUrl)(FakeRequest())
      contentAsString(page) should include("No thanks. Keep sending me reminders by letter")
      verify(controller.saMicroService, times(1)).getPreferences(validUtr)
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

    "A post to keep paper notification" should {

      "redirect to the portal" in new WithApplication(FakeApplication()) {
        val controller = createController

        val page = controller.submitKeepPaperForm(validToken, validReturnUrl)(FakeRequest())

        status(page) shouldBe 303
        header("Location", page).get should include(validReturnUrl)
      }
    }

    "save the user preference to keep the paper notification" in new WithApplication(FakeApplication()) {
      val controller = createController

      controller.submitKeepPaperForm(validToken, validReturnUrl)(FakeRequest())
      verify(controller.saMicroService, times(1)).savePreferences(validUtr, false)
    }

  }
}