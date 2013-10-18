package controllers

import org.scalatest.{BeforeAndAfter, ShouldMatchers, WordSpec}
import play.api.test.{ FakeApplication, WithApplication }
import play.api.test.FakeRequest
import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers._
import org.mockito.Mockito._
import uk.gov.hmrc.{ SaPreference, PreferencesMicroService }
import org.joda.time.{ DateTimeZone, DateTime }
import java.net.URLEncoder
import controllers.service.RedirectWhiteListService
import java.net.URL
import org.mockito.Mockito

class SaPrefControllerSpec extends WordSpec with ShouldMatchers with MockitoSugar with BeforeAndAfter {

  import play.api.test.Helpers._

  val validUtr = "1234567"
  lazy val validToken = URLEncoder.encode(SsoPayloadEncryptor.encrypt(s"$validUtr:${DateTime.now(DateTimeZone.UTC).getMillis}"), "UTF-8")
  lazy val expiredToken = URLEncoder.encode(SsoPayloadEncryptor.encrypt(s"$validUtr:${DateTime.now(DateTimeZone.UTC).minusDays(1).getMillis}"), "UTF-8")
  val validReturnUrl = URLEncoder.encode("http://localhost:8080/portal", "UTF-8")
  private val mockRedirectWhiteListService = mock[RedirectWhiteListService]

  def createController = new SaPrefsController {
    override val redirectWhiteListService = mockRedirectWhiteListService
    override lazy val preferencesMicroService = mock[PreferencesMicroService]
  }

  before {
    Mockito.reset(mockRedirectWhiteListService)
  }

  "Preferences pages" should {
    "redirect to the portal when preferences already exist for a specific utr" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)

      val controller = createController
      val preferencesAlreadyCreated = SaPreference(true, Some("test@test.com"))
      when(controller.preferencesMicroService.getPreferences(validUtr)).thenReturn(Some(preferencesAlreadyCreated))

      val page = controller.index(validToken, validReturnUrl)(FakeRequest())
      status(page) shouldBe 303
      header("Location", page).get should include(validReturnUrl)
      verify(controller.preferencesMicroService, times(1)).getPreferences(validUtr)
    }

    "render an email input field" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)
      val controller = createController
      when(controller.preferencesMicroService.getPreferences(validUtr)).thenReturn(None)

      val page = controller.index(validToken, validReturnUrl)(FakeRequest())
      contentAsString(page) should include("email")
      verify(controller.preferencesMicroService, times(1)).getPreferences(validUtr)
    }

    "redirect to portal if the token is expired on the landing page" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)
      val controller = createController

      val page = controller.index(expiredToken, validReturnUrl)(FakeRequest())

      status(page) shouldBe 303
      header("Location", page).get should include(validReturnUrl)
    }

    "include a link to keep mail preference" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)
      val controller = createController
      when(controller.preferencesMicroService.getPreferences(validUtr)).thenReturn(None)

      val page = controller.index(validToken, validReturnUrl)(FakeRequest())
      contentAsString(page) should include("No thanks. Keep sending me reminders by letter")
      verify(controller.preferencesMicroService, times(1)).getPreferences(validUtr)
    }

    "return bad request if redirect_url is not in the whitelist" in {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(false)
      val controller = createController

      val page = controller.index(validToken, validReturnUrl)(FakeRequest())

      status(page) shouldBe 500
    }
  }

  "A post to set preferences" should {

    "redirect to return url if the token is expired when submitting the form" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)

      val controller = createController

      val page = controller.submitPrefsForm(expiredToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email", "foo@bar.com")))
      verify(controller.preferencesMicroService, times(0)).savePreferences(any[String], any[Boolean], any[Option[String]])

      status(page) shouldBe 303
      header("Location", page).get should include(validReturnUrl)
    }

    "redirect to a confirmation page" in new WithApplication(FakeApplication()) {
      val controller = createController
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)
      when(controller.preferencesMicroService.getPreferences(validUtr)).thenReturn(None)

      val page = controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email", "foo@bar.com")))

      verify(controller.preferencesMicroService).getPreferences(validUtr)
      status(page) shouldBe 303
      header("Location", page).get should include(s"/sa/print-preferences-saved?return_url=$validReturnUrl")
    }

    "show an error if the email is invalid" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)
      val controller = createController

      val page = controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email", "invalid-email")))

      status(page) shouldBe 400
      contentAsString(page) should include("Please provide a valid email address")
      verifyZeroInteractions(controller.preferencesMicroService)
    }

    "show an error if the email is not set" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)
      val controller = createController

      val page = controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email", "")))

      status(page) shouldBe 400
      contentAsString(page) should include("Please provide a valid email address")
      verify(controller.preferencesMicroService, times(0)).savePreferences(any[String], any[Boolean], any[Option[String]])
    }

    "save the user preferences" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)

      val controller = createController
      when(controller.preferencesMicroService.getPreferences(validUtr)).thenReturn(None)

      controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email", "foo@bar.com")))
      verify(controller.preferencesMicroService, times(1)).savePreferences(validUtr, true, Some("foo@bar.com"))
    }

    "redirect to no-action page if the preference is already set to digital when submitting the form" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)

      val controller = createController

      when(controller.preferencesMicroService.getPreferences(validUtr)).thenReturn(Some(SaPreference(true, Some("foo@bar.com"))))

      val action = controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email", "foo@bar.com")))

      verify(controller.preferencesMicroService, times(1)).getPreferences(validUtr)
      verify(controller.preferencesMicroService, times(0)).savePreferences(any[String], any[Boolean], any[Option[String]])

      status(action) shouldBe 303
      header("Location", action).get should include("/sa/print-preferences-no-action")
      header("Location", action).get should include("digital=true")
    }

    "redirect to no-action page if the preference is already set to paper when submitting the form" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)

      val controller = createController

      when(controller.preferencesMicroService.getPreferences(validUtr)).thenReturn(Some(SaPreference(false, None)))

      val action = controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email", "foo@bar.com")))

      verify(controller.preferencesMicroService, times(1)).getPreferences(validUtr)
      verify(controller.preferencesMicroService, times(0)).savePreferences(any[String], any[Boolean], any[Option[String]])

      status(action) shouldBe 303
      header("Location", action).get should include("/sa/print-preferences-no-action")
      header("Location", action).get should include("digital=false")
    }

    "return bad request if redirect_url is not in the whitelist" in {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(false)
      val controller = createController

      val page = controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest())

      status(page) shouldBe 500
    }

    "keep paper notification and redirect to the portal" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)
      val controller = createController
      when(controller.preferencesMicroService.getPreferences(validUtr)).thenReturn(None)

      val page = controller.submitKeepPaperForm(validToken, validReturnUrl)(FakeRequest())

      status(page) shouldBe 303
      header("Location", page).get should include(validReturnUrl)
    }

    "save the user preference to keep the paper notification" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)
      val controller = createController
      when(controller.preferencesMicroService.getPreferences(validUtr)).thenReturn(None)

      controller.submitKeepPaperForm(validToken, validReturnUrl)(FakeRequest())
      verify(controller.preferencesMicroService, times(1)).savePreferences(validUtr, false)
    }

    "redirect to return url if the token is expired when the keep paper notification form is used" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)

      val controller = createController

      val page = controller.submitKeepPaperForm(expiredToken, validReturnUrl)(FakeRequest())
      verify(controller.preferencesMicroService, times(0)).savePreferences(any[String], any[Boolean], any[Option[String]])

      status(page) shouldBe 303
      header("Location", page).get should include(validReturnUrl)
    }

    "redirect to no-action page if the preference is already set to digital when the keep paper notification form is used" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)

      val controller = createController

      when(controller.preferencesMicroService.getPreferences(validUtr)).thenReturn(Some(SaPreference(true, Some("foo@bar.com"))))

      val action = controller.submitKeepPaperForm(validToken, validReturnUrl)(FakeRequest())

      verify(controller.preferencesMicroService, times(1)).getPreferences(validUtr)
      verify(controller.preferencesMicroService, times(0)).savePreferences(any[String], any[Boolean], any[Option[String]])

      status(action) shouldBe 303
      header("Location", action).get should include("/sa/print-preferences-no-action")
      header("Location", action).get should include("digital=true")
    }

    "redirect to no-action page if the preference is already set to paper when the keep paper notification form is used" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)

      val controller = createController

      when(controller.preferencesMicroService.getPreferences(validUtr)).thenReturn(Some(SaPreference(false, None)))

      val action = controller.submitKeepPaperForm(validToken, validReturnUrl)(FakeRequest())

      verify(controller.preferencesMicroService, times(1)).getPreferences(validUtr)
      verify(controller.preferencesMicroService, times(0)).savePreferences(any[String], any[Boolean], any[Option[String]])

      status(action) shouldBe 303
      header("Location", action).get should include("/sa/print-preferences-no-action")
      header("Location", action).get should include("digital=false")
    }
  }
}