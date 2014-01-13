package controllers

import org.scalatest.{BeforeAndAfter, ShouldMatchers, WordSpec}
import play.api.test.{ FakeApplication, WithApplication }
import play.api.test.FakeRequest
import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers._
import org.mockito.Mockito._
import uk.gov.hmrc.{EmailConnector, SaPreference, PreferencesConnector}
import org.joda.time.{ DateTimeZone, DateTime }
import java.net.URLEncoder
import org.mockito.Mockito
import org.jsoup.Jsoup
import uk.gov.hmrc.SaPreference
import scala.Some
import play.api.test.FakeApplication
import scala.concurrent.Future
import controllers.sa.prefs.service.RedirectWhiteListService
import controllers.sa.prefs.{SsoPayloadEncryptor, SaPrefsController}

class SaPrefControllerSpec extends WordSpec with ShouldMatchers with MockitoSugar with BeforeAndAfter {

  import play.api.test.Helpers._

  val validUtr = "1234567"
  lazy val validToken = URLEncoder.encode(SsoPayloadEncryptor.encrypt(s"$validUtr:${DateTime.now(DateTimeZone.UTC).getMillis}"), "UTF-8")
  lazy val expiredToken = URLEncoder.encode(SsoPayloadEncryptor.encrypt(s"$validUtr:${DateTime.now(DateTimeZone.UTC).minusDays(1).getMillis}"), "UTF-8")
  lazy val incorrectToken = "this is an incorrect token khdskjfhasduiy3784y37yriuuiyr3i7rurkfdsfhjkdskh"
  val validReturnUrl = URLEncoder.encode("http://localhost:8080/portal", "UTF-8")
  private val mockRedirectWhiteListService = mock[RedirectWhiteListService]

  def createController = new SaPrefsController {
    override val redirectWhiteListService = mockRedirectWhiteListService
    override lazy val preferencesConnector = mock[PreferencesConnector]
    override lazy val emailConnector = mock[EmailConnector]
  }

  before {
    Mockito.reset(mockRedirectWhiteListService)
  }

  "Preferences pages" should {
    "redirect to the portal when preferences already exist for a specific utr" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)

      val controller = createController
      val preferencesAlreadyCreated = SaPreference(true, Some("test@test.com"))
      when(controller.preferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(Some(preferencesAlreadyCreated)))

      val page = controller.index(validToken, validReturnUrl, None)(FakeRequest())
      status(page) shouldBe 303
      header("Location", page).get should include(validReturnUrl)
      verify(controller.preferencesConnector, times(1)).getPreferences(validUtr)
    }

    "render an email input field" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)
      val controller = createController
      when(controller.preferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(None))

      val page = controller.index(validToken, validReturnUrl, None)(FakeRequest())
      contentAsString(page) should include("email.main")
      verify(controller.preferencesConnector, times(1)).getPreferences(validUtr)
    }

    "redirect to portal if the token is expired on the landing page" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)
      val controller = createController

      val page = controller.index(expiredToken, validReturnUrl, None)(FakeRequest())

      status(page) shouldBe 303
      header("Location", page).get should include(validReturnUrl)
    }

    "redirect to portal if the token is not valid on the landing page" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)
      val controller = createController

      val page = controller.index(incorrectToken, validReturnUrl, None)(FakeRequest())

      status(page) shouldBe 303
      header("Location", page).get should include(validReturnUrl)
    }

    "include a link to keep mail preference" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)
      val controller = createController
      when(controller.preferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(None))

      val page = controller.index(validToken, validReturnUrl, None)(FakeRequest())
      contentAsString(page) should include("No thanks, I don’t want to switch to email")
      verify(controller.preferencesConnector, times(1)).getPreferences(validUtr)
    }

    "return bad request if redirect_url is not in the whitelist" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(false)
      val controller = createController

      val page = controller.index(validToken, validReturnUrl, None)(FakeRequest())

      status(page) shouldBe 500
    }

    "fill the email form if user is coming from the warning page" in new WithApplication(FakeApplication()) {
      val controller = createController
      val previouslyEnteredAddress = "some@mail.com"

      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)
      when(controller.preferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(None))
      val page = controller.index(validToken, validReturnUrl, Some(previouslyEnteredAddress))(FakeRequest())

      status(page) shouldBe 200
      val html = Jsoup.parse(contentAsString(page))
      html.getElementById("email.main") shouldNot be(null)
      html.getElementById("email.main").`val` shouldBe previouslyEnteredAddress
      html.getElementById("email.confirm") shouldNot be(null)
      html.getElementById("email.confirm").`val` shouldBe previouslyEnteredAddress
    }
  }

  private val emailAddress = "foo@bar.com"

  "A post to set preferences" should {

    "redirect to return url if the token is expired when submitting the form" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)

      val controller = createController

      val page = controller.submitPrefsForm(expiredToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress)))

      status(page) shouldBe 303

      verify(controller.preferencesConnector, times(0)).savePreferences(any[String], any[Boolean], any[Option[String]])

      header("Location", page).get should include(validReturnUrl)
    }

    "return a warning page if the email address could not be verified" in new WithApplication(FakeApplication()) {
      val controller = createController
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)

      when(controller.emailConnector.validateEmailAddress(emailAddress)).thenReturn(Future.successful(false))

      val page = controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress)))

      status(page) shouldBe 200

      verify(controller.emailConnector).validateEmailAddress(emailAddress)
      verifyZeroInteractions(controller.preferencesConnector)

    }

    "show an error if the email is invalid" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)
      val controller = createController

      val page = controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email.main", "invalid-email"), ("email.confirm", "")))

      status(page) shouldBe 400
      contentAsString(page) should include("Please provide a valid email address")
      verifyZeroInteractions(controller.preferencesConnector, controller.emailConnector)
    }

    "show an error if the email is not set" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)
      val controller = createController

      val page = controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email.main", ""), ("email.confirm", "")))

      status(page) shouldBe 400
      contentAsString(page) should include("Please provide a valid email address")
      verify(controller.preferencesConnector, times(0)).savePreferences(any[String], any[Boolean], any[Option[String]])
    }

    "show an error if the confirmed email is not the same as the main" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)
      val controller = createController

      val page = controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email.main", "valid@mail.com"), ("email.confirm", "notMatching@mail.com")))

      status(page) shouldBe 400
      contentAsString(page) should include("The email addresses entered do not match")
      verify(controller.preferencesConnector, times(0)).savePreferences(any[String], any[Boolean], any[Option[String]])
    }

    "save the user preferences" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)

      val controller = createController
      when(controller.emailConnector.validateEmailAddress(emailAddress)).thenReturn(Future.successful(true))
      when(controller.preferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(None))

      val page = controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress), ("email.confirm", emailAddress)))

      status(page) shouldBe 303

      verify(controller.emailConnector).validateEmailAddress(emailAddress)
      verify(controller.preferencesConnector).getPreferences(validUtr)
      verify(controller.preferencesConnector).savePreferences(validUtr, true, Some(emailAddress))
    }

    "redirect to no-action page if the preference is already set to digital when submitting the form" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)

      val controller = createController
      when(controller.emailConnector.validateEmailAddress(emailAddress)).thenReturn(Future.successful(true))
      when(controller.preferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(Some(SaPreference(true, Some(emailAddress)))))

      val action = controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress)))

      status(action) shouldBe 303

      verify(controller.emailConnector).validateEmailAddress(emailAddress)
      verify(controller.preferencesConnector, times(1)).getPreferences(validUtr)
      verify(controller.preferencesConnector, times(0)).savePreferences(any[String], any[Boolean], any[Option[String]])

      header("Location", action).get should include("/sa/print-preferences-no-action")
      header("Location", action).get should include("digital=true")
    }

    "redirect to no-action page if the preference is already set to paper when submitting the form" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)

      val controller = createController
      when(controller.emailConnector.validateEmailAddress(emailAddress)).thenReturn(Future.successful(true))
      when(controller.preferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(Some(SaPreference(false, None))))

      val action = controller.submitPrefsForm(validToken, validReturnUrl)(FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress)))

      status(action) shouldBe 303

      verify(controller.emailConnector).validateEmailAddress(emailAddress)
      verify(controller.preferencesConnector, times(1)).getPreferences(validUtr)
      verify(controller.preferencesConnector, times(0)).savePreferences(any[String], any[Boolean], any[Option[String]])

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
      when(controller.preferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(None))

      val page = controller.submitKeepPaperForm(validToken, validReturnUrl)(FakeRequest())

      status(page) shouldBe 303
      header("Location", page).get should include(validReturnUrl)
    }

    "save the user preference to keep the paper notification" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)
      val controller = createController
      when(controller.preferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(None))

      val result = controller.submitKeepPaperForm(validToken, validReturnUrl)(FakeRequest())

      status(result) shouldBe 303

      verify(controller.preferencesConnector, times(1)).savePreferences(validUtr, false)
    }

    "redirect to return url if the token is expired when the keep paper notification form is used" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)

      val controller = createController

      val page = controller.submitKeepPaperForm(expiredToken, validReturnUrl)(FakeRequest())

      status(page) shouldBe 303

      verify(controller.preferencesConnector, times(0)).savePreferences(any[String], any[Boolean], any[Option[String]])
      header("Location", page).get should include(validReturnUrl)
    }

    "redirect to no-action page if the preference is already set to digital when the keep paper notification form is used" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)

      val controller = createController

      when(controller.preferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(Some(SaPreference(true, Some(emailAddress)))))

      val action = controller.submitKeepPaperForm(validToken, validReturnUrl)(FakeRequest())

      status(action) shouldBe 303

      verify(controller.preferencesConnector, times(1)).getPreferences(validUtr)
      verify(controller.preferencesConnector, times(0)).savePreferences(any[String], any[Boolean], any[Option[String]])

      header("Location", action).get should include("/sa/print-preferences-no-action")
      header("Location", action).get should include("digital=true")
    }

    "redirect to no-action page if the preference is already set to paper when the keep paper notification form is used" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(validReturnUrl)).thenReturn(true)

      val controller = createController

      when(controller.preferencesConnector.getPreferences(validUtr)).thenReturn(Future.successful(Some(SaPreference(false, None))))

      val action = controller.submitKeepPaperForm(validToken, validReturnUrl)(FakeRequest())

      status(action) shouldBe 303

      verify(controller.preferencesConnector, times(1)).getPreferences(validUtr)
      verify(controller.preferencesConnector, times(0)).savePreferences(any[String], any[Boolean], any[Option[String]])

      header("Location", action).get should include("/sa/print-preferences-no-action")
      header("Location", action).get should include("digital=false")
    }
  }
}