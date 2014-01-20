package controllers

import org.scalatest.{BeforeAndAfter, ShouldMatchers, WordSpec}
import play.api.test.WithApplication
import play.api.test.FakeRequest
import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers
import Matchers.{eq => meq, any}
import org.mockito.Mockito._
import org.joda.time.{DateTimeZone, DateTime}
import java.net.URLDecoder.{decode => urlDecode}
import java.net.URLEncoder.{encode => urlEncode}
import org.mockito.Mockito
import org.jsoup.Jsoup
import scala.concurrent.Future
import controllers.sa.prefs.service.{SsoPayloadCrypto, RedirectWhiteListService}
import SsoPayloadCrypto._
import controllers.sa.prefs.SaPrefsController
import controllers.common.actions.HeaderCarrier
import scala.Some
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.email.EmailConnector
import uk.gov.hmrc.common.microservice.preferences.{SaEmailPreference, SaPreference, PreferencesConnector}
import SaEmailPreference.Status
import uk.gov.hmrc.common.microservice.MicroServiceException
import org.scalatest.concurrent.ScalaFutures

class SaPrefControllerSpec extends WordSpec with ShouldMatchers with MockitoSugar with BeforeAndAfter with ScalaFutures {

  import play.api.test.Helpers._

  val validUtr = "1234567"
  lazy val validToken = urlEncode(encrypt(s"$validUtr:${DateTime.now(DateTimeZone.UTC).getMillis}"), "UTF-8")
  lazy val expiredToken = urlEncode(encrypt(s"$validUtr:${DateTime.now(DateTimeZone.UTC).minusDays(1).getMillis}"), "UTF-8")
  lazy val incorrectToken = "this is an incorrect token khdskjfhasduiy3784y37yriuuiyr3i7rurkfdsfhjkdskh"
  val decodedReturnUrl = "http://localhost:8080/portal"
  val encodedReturnUrl = urlEncode(decodedReturnUrl, "UTF-8")
  private val mockRedirectWhiteListService = mock[RedirectWhiteListService]

  def createController = new SaPrefsController {
    override val redirectWhiteListService = mockRedirectWhiteListService
    override lazy val preferencesConnector = mock[PreferencesConnector]
    override lazy val emailConnector = mock[EmailConnector]
  }

  before {
    Mockito.reset(mockRedirectWhiteListService)
  }

  val request = FakeRequest()

  implicit def hc: HeaderCarrier = any()


  "Preferences pages" should {
    "redirect to the portal when preferences already exist for a specific utr" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)

      val controller = createController
      val preferencesAlreadyCreated = SaPreference(true, Some(SaEmailPreference("test@test.com", status = Status.verified)))
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(preferencesAlreadyCreated)))

      val page = controller.index(validToken, encodedReturnUrl, None)(request)
      status(page) shouldBe 303
      header("Location", page).get should equal(encodedReturnUrl)
      verify(controller.preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
    }

    "render an email input field" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)
      val controller = createController
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))(any())).thenReturn(Future.successful(None))

      val page = controller.index(validToken, encodedReturnUrl, None)(request)
      contentAsString(page) should include("email.main")
      verify(controller.preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
    }

    "redirect to portal if the token is expired on the landing page" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)
      val controller = createController

      val page = controller.index(expiredToken, encodedReturnUrl, None)(request)

      status(page) shouldBe 303
      header("Location", page).get should equal(encodedReturnUrl)
    }

    "redirect to portal if the token is not valid on the landing page" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)
      val controller = createController

      val page = controller.index(incorrectToken, encodedReturnUrl, None)(request)

      status(page) shouldBe 303
      header("Location", page).get should equal(encodedReturnUrl)
    }

    "include a link to keep mail preference" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)
      val controller = createController
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(None))

      val page = controller.index(validToken, encodedReturnUrl, None)(request)
      contentAsString(page) should include("No thanks, I don’t want to switch to email")
      verify(controller.preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))(any())
    }

    "return bad request if redirect_url is not in the whitelist" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(false)
      val controller = createController

      val page = controller.index(validToken, encodedReturnUrl, None)(request)

      status(page) shouldBe 500
    }

    "fill the email form if user is coming from the warning page" in new WithApplication(FakeApplication()) {
      val controller = createController
      val previouslyEnteredAddress = "some@mail.com"

      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(None))
      val page = controller.index(validToken, encodedReturnUrl, Some(previouslyEnteredAddress))(request)

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
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)

      val controller = createController

      implicit val request = FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress))
      val page = controller.submitPrefsForm(expiredToken, encodedReturnUrl)(request)

      status(page) shouldBe 303

      verify(controller.preferencesConnector, times(0)).savePreferencesUnsecured(any[String], any[Boolean], any[Option[String]])

      header("Location", page).get should equal(encodedReturnUrl)
    }

    "return a warning page if the email address could not be verified" in new WithApplication(FakeApplication()) {
      val controller = createController
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)

      when(controller.emailConnector.validateEmailAddress(meq(emailAddress))).thenReturn(Future.successful(false))

      implicit val request = FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress))
      val page = controller.submitPrefsForm(validToken, encodedReturnUrl)(request)

      status(page) shouldBe 200

      verify(controller.emailConnector).validateEmailAddress(meq(emailAddress))
      verifyZeroInteractions(controller.preferencesConnector)

    }

    "show an error if the email is invalid" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)
      val controller = createController

      implicit val request = FakeRequest().withFormUrlEncodedBody(("email.main", "invalid-email"), ("email.confirm", ""))
      val page = controller.submitPrefsForm(validToken, encodedReturnUrl)(request)

      status(page) shouldBe 400
      contentAsString(page) should include("Enter a valid email address")
      verifyZeroInteractions(controller.preferencesConnector, controller.emailConnector)
    }

    "show an error if the email is not set" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)
      val controller = createController

      implicit val request = FakeRequest().withFormUrlEncodedBody(("email.main", ""), ("email.confirm", ""))
      val page = controller.submitPrefsForm(validToken, encodedReturnUrl)(request)

      status(page) shouldBe 400
      contentAsString(page) should include("Enter a valid email address")
      verify(controller.preferencesConnector, times(0)).savePreferencesUnsecured(any[String], any[Boolean], any[Option[String]])
    }

    "show an error if the confirmed email is not the same as the main" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)
      val controller = createController

      implicit val request = FakeRequest().withFormUrlEncodedBody(("email.main", "valid@mail.com"), ("email.confirm", "notMatching@mail.com"))
      val page = controller.submitPrefsForm(validToken, encodedReturnUrl)(request)

      status(page) shouldBe 400
      contentAsString(page) should include("Check your email addresses - they don&#x27;t match.")
      verify(controller.preferencesConnector, times(0)).savePreferencesUnsecured(any[String], any[Boolean], any[Option[String]])
    }

    "save the user preferences" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)

      val controller = createController
      when(controller.emailConnector.validateEmailAddress(meq(emailAddress))).thenReturn(Future.successful(true))
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(None))
      when(controller.preferencesConnector.savePreferencesUnsecured(meq(validUtr), meq(true), meq(Some(emailAddress)))).thenReturn(Future.successful(None))

      implicit val request = FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress), ("email.confirm", emailAddress))
      val page = controller.submitPrefsForm(validToken, encodedReturnUrl)(request)

      status(page) shouldBe 303

      verify(controller.emailConnector).validateEmailAddress(meq(emailAddress))
      verify(controller.preferencesConnector).getPreferencesUnsecured(meq(validUtr))
      verify(controller.preferencesConnector).savePreferencesUnsecured(meq(validUtr), meq(true), meq(Some(emailAddress)))
    }

    "generate an error if the preferences could not be saved" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)

      val controller = createController
      when(controller.emailConnector.validateEmailAddress(meq(emailAddress))).thenReturn(Future.successful(true))
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(None))
      when(controller.preferencesConnector.savePreferencesUnsecured(meq(validUtr), meq(true), meq(Some(emailAddress)))).thenReturn(Future.failed(new RuntimeException()))

      implicit val request = FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress), ("email.confirm", emailAddress))
      evaluating {
        controller.submitPrefsForm(validToken, encodedReturnUrl)(request).futureValue
      } should produce [RuntimeException]
    }

    "redirect to no-action page if the preference is already set to digital when submitting the form" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)

      val controller = createController
      when(controller.emailConnector.validateEmailAddress(meq(emailAddress))).thenReturn(Future.successful(true))
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(SaPreference(true, Some(SaEmailPreference(emailAddress, Status.verified))))))

      implicit val request = FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress))
      val action = controller.submitPrefsForm(validToken, encodedReturnUrl)(request)

      status(action) shouldBe 303

      verify(controller.emailConnector).validateEmailAddress(meq(emailAddress))
      verify(controller.preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
      verify(controller.preferencesConnector, times(0)).savePreferencesUnsecured(any[String], any[Boolean], any[Option[String]])

      header("Location", action).get should include("/sa/print-preferences-no-action")
      header("Location", action).get should include("digital=true")
    }

    "redirect to no-action page if the preference is already set to paper when submitting the form" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)

      val controller = createController
      when(controller.emailConnector.validateEmailAddress(meq(emailAddress))).thenReturn(Future.successful(true))
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(SaPreference(false, None))))

      implicit val request = FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress))
      val action = controller.submitPrefsForm(validToken, encodedReturnUrl)(request)

      status(action) shouldBe 303
      status(action) shouldBe 303

      verify(controller.emailConnector).validateEmailAddress(meq(emailAddress))
      verify(controller.preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
      verify(controller.preferencesConnector, times(0)).savePreferencesUnsecured(any[String], any[Boolean], any[Option[String]])

      header("Location", action).get should include("/sa/print-preferences-no-action")
      header("Location", action).get should include("digital=false")
    }

    "return bad request if redirect_url is not in the whitelist" in {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(false)
      val controller = createController

      val page = controller.submitPrefsForm(validToken, encodedReturnUrl)(request)

      status(page) shouldBe 500
    }

    "keep paper notification and redirect to the portal" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)
      val controller = createController
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(None))

      val page = controller.submitKeepPaperForm(validToken, encodedReturnUrl)(request)

      status(page) shouldBe 303
      header("Location", page).get should equal(encodedReturnUrl)
    }

    "save the user preference to keep the paper notification" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)
      val controller = createController
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(None))

      val result = controller.submitKeepPaperForm(validToken, encodedReturnUrl)(request)

      status(result) shouldBe 303

      verify(controller.preferencesConnector, times(1)).savePreferencesUnsecured(meq(validUtr), meq(false), meq(null))
    }

    "redirect to return url if the token is expired when the keep paper notification form is used" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)

      val controller = createController

      val page = controller.submitKeepPaperForm(expiredToken, encodedReturnUrl)(request)

      status(page) shouldBe 303

      verify(controller.preferencesConnector, times(0)).savePreferencesUnsecured(any[String], any[Boolean], any[Option[String]])
      header("Location", page).get should equal(encodedReturnUrl)
    }

    "redirect to no-action page if the preference is already set to digital when the keep paper notification form is used" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)

      val controller = createController

      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(SaPreference(true, Some(SaEmailPreference(emailAddress, Status.verified))))))

      val action = controller.submitKeepPaperForm(validToken, encodedReturnUrl)(request)

      status(action) shouldBe 303

      verify(controller.preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
      verify(controller.preferencesConnector, times(0)).savePreferencesUnsecured(any[String], any[Boolean], any[Option[String]])

      header("Location", action).get should include("/sa/print-preferences-no-action")
      header("Location", action).get should include("digital=true")
    }

    "redirect to no-action page if the preference is already set to paper when the keep paper notification form is used" in new WithApplication(FakeApplication()) {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)

      val controller = createController

      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(SaPreference(false, None))))

      val action = controller.submitKeepPaperForm(validToken, encodedReturnUrl)(request)

      status(action) shouldBe 303

      verify(controller.preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
      verify(controller.preferencesConnector, times(0)).savePreferencesUnsecured(any[String], any[Boolean], any[Option[String]])

      header("Location", action).get should include("/sa/print-preferences-no-action")
      header("Location", action).get should include("digital=false")
    }
  }

  "The confirm preferences set page" should {
    "reject an invalid return url" in {
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(false)
      val controller = createController
      val result = controller.confirm(validToken, encodedReturnUrl)(request)
      status(result) should be(500) // FIXME change to Bad Request
    }
    "contain a link with the return url" in {
      val controller = createController
      when(mockRedirectWhiteListService.check(encodedReturnUrl)).thenReturn(true)
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(
        Future.successful(Some(SaPreference(true, Some(SaEmailPreference(emailAddress, Status.pending)))))
      )
      val result = controller.confirm(validToken, encodedReturnUrl)(request)
      status(result) should be(200)

      val page = Jsoup.parse(contentAsString(result))
      val returnUrl = page.getElementById("sa-home-link").attr("href")
      returnUrl should be (s"$decodedReturnUrl?emailAddress=${urlEncode(encrypt(emailAddress))}")
    }
    "generate an error if the user does not have an email address set" in pending
    "handle return urls which already have query parameters" in pending
  }
}