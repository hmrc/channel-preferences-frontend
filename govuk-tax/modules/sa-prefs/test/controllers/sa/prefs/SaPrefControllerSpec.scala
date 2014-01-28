package controllers.sa.prefs

import org.scalatest.{OptionValues, BeforeAndAfter, ShouldMatchers, WordSpec}
import play.api.test.WithApplication
import play.api.test.FakeRequest
import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.joda.time.{DateTimeZone, DateTime}
import java.net.URLDecoder.{decode => urlDecode}
import java.net.URLEncoder.{encode => urlEncode}
import org.jsoup.Jsoup
import scala.concurrent.Future
import controllers.sa.prefs.service.SsoPayloadCrypto
import SsoPayloadCrypto._
import controllers.sa.prefs.SaPrefsController
import controllers.common.actions.HeaderCarrier
import scala.Some
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.email.EmailConnector
import uk.gov.hmrc.common.microservice.preferences._
import SaEmailPreference.Status
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.domain.SaUtr
import scala.Some
import uk.gov.hmrc.domain.SaUtr
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.preferences.SaPreference
import scala.Some
import uk.gov.hmrc.domain.SaUtr
import play.api.test.FakeApplication
import java.net.URI

class SaPrefControllerSpec extends WordSpec with ShouldMatchers with MockitoSugar with BeforeAndAfter with ScalaFutures with OptionValues {

  import play.api.test.Helpers._

  val emailAddress = "foo@bar.com"
  val validUtr = SaUtr("1234567")
  lazy val validToken = urlEncode(encrypt(s"$validUtr:${DateTime.now(DateTimeZone.UTC).getMillis}"), "UTF-8")
  lazy val expiredToken = urlEncode(encrypt(s"$validUtr:${DateTime.now(DateTimeZone.UTC).minusDays(1).getMillis}"), "UTF-8")
  lazy val incorrectToken = "this is an incorrect token khdskjfhasduiy3784y37yriuuiyr3i7rurkfdsfhjkdskh"
  val decodedReturnUrl = "http://localhost:8080/portal?exampleQuery=exampleValue"
  val encodedReturnUrl = urlEncode(decodedReturnUrl, "UTF-8")
  lazy val decodedReturnUrlWithEmailAddress = s"$decodedReturnUrl&emailAddress=${urlEncode(encrypt(emailAddress), "UTF-8")}"
  val encodedUrlNotOnWhitelist = urlEncode("http://notOnWhiteList/something", "UTF-8")

  def createController = new SaPrefsController(whiteList = Set("localhost")) {
    override lazy val preferencesConnector = mock[PreferencesConnector]
    override lazy val emailConnector = mock[EmailConnector]
  }

  val request = FakeRequest()

  implicit def hc: HeaderCarrier = any()


  "Preferences pages" should {
    "redirect to the portal when a preference for email already exists for a specific utr" in new WithApplication(FakeApplication()) {
      val controller = createController
      val preferencesAlreadyCreated = SaPreference(true, Some(SaEmailPreference(emailAddress, status = Status.verified)))
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(preferencesAlreadyCreated)))

      val page = controller.index(validToken, encodedReturnUrl, None)(request)
      status(page) shouldBe 303
      header("Location", page).value should be (decodedReturnUrlWithEmailAddress)
      verify(controller.preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
    }

    "redirect to the portal when a preference for paper already exists for a specific utr" in new WithApplication(FakeApplication()) {
      val controller = createController
      val preferencesAlreadyCreated = SaPreference(false, None)
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(preferencesAlreadyCreated)))

      val page = controller.index(validToken, encodedReturnUrl, None)(request)
      status(page) shouldBe 303
      header("Location", page).value should be (decodedReturnUrl)
      verify(controller.preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
    }

    "redirect to the portal when preferences already exist for a specific utr and an email address was passed to the platform" in new WithApplication(FakeApplication()) {
      val controller = createController
      val preferencesAlreadyCreated = SaPreference(true, Some(SaEmailPreference(emailAddress, status = Status.verified)))
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(preferencesAlreadyCreated)))

      val page = controller.index(validToken, encodedReturnUrl, Some("other@me.com"))(request)
      status(page) shouldBe 303
      header("Location", page).value should be (decodedReturnUrlWithEmailAddress)
    }

    "render an email input field" in new WithApplication(FakeApplication()) {
      val controller = createController
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))(any())).thenReturn(Future.successful(None))

      val page = controller.index(validToken, encodedReturnUrl, None)(request)
      contentAsString(page) should include("email.main")
      verify(controller.preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
    }

    "redirect to portal if the token is expired on the landing page" in new WithApplication(FakeApplication()) {
      val controller = createController

      val page = controller.index(expiredToken, encodedReturnUrl, None)(request)

      status(page) shouldBe 303
      header("Location", page).get should equal(decodedReturnUrl)
    }

    "redirect to portal if the token is not valid on the landing page" in new WithApplication(FakeApplication()) {
      val controller = createController

      val page = controller.index(incorrectToken, encodedReturnUrl, None)(request)

      status(page) shouldBe 303
      header("Location", page).get should equal(decodedReturnUrl)
    }

    "include a link to keep mail preference" in new WithApplication(FakeApplication()) {
      val controller = createController
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(None))

      val page = controller.index(validToken, encodedReturnUrl, None)(request)
      val html = Jsoup.parse(contentAsString(page))
      html.getElementById("keep-paper-link") shouldNot be(null)
      verify(controller.preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))(any())
    }

    "return bad request if redirect_url is not in the whitelist" in new WithApplication(FakeApplication()) {
      val controller = createController
      val page = controller.index(validToken, encodedUrlNotOnWhitelist, None)(request)
      status(page) shouldBe 400
    }

    "fill the email form if user is coming from the warning page" in new WithApplication(FakeApplication()) {
      val controller = createController
      val previouslyEnteredAddress = "some@mail.com"

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

  "A post to set preferences" should {

    "redirect to return url if the token is expired when submitting the form" in new WithApplication(FakeApplication()) {

      val controller = createController

      implicit val request = FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress))
      val page = controller.submitPrefsForm(expiredToken, encodedReturnUrl)(request)

      status(page) shouldBe 303

      verify(controller.preferencesConnector, times(0)).savePreferencesUnsecured(any[SaUtr], any[Boolean], any[Option[String]])

      header("Location", page).get should equal(decodedReturnUrl)
    }

    "return a warning page if the email address could not be verified" in new WithApplication(FakeApplication()) {
      val controller = createController

      when(controller.emailConnector.validateEmailAddress(meq(emailAddress))).thenReturn(Future.successful(false))

      implicit val request = FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress))
      val page = controller.submitPrefsForm(validToken, encodedReturnUrl)(request)

      status(page) shouldBe 200

      verify(controller.emailConnector).validateEmailAddress(meq(emailAddress))
      verifyZeroInteractions(controller.preferencesConnector)

    }

    "show an error if the email is invalid" in new WithApplication(FakeApplication()) {
      val controller = createController

      implicit val request = FakeRequest().withFormUrlEncodedBody(("email.main", "invalid-email"), ("email.confirm", ""))
      val page = controller.submitPrefsForm(validToken, encodedReturnUrl)(request)

      status(page) shouldBe 400
      contentAsString(page) should include("Enter a valid email address")
      verifyZeroInteractions(controller.preferencesConnector, controller.emailConnector)
    }

    "show an error if the email is not set" in new WithApplication(FakeApplication()) {
      val controller = createController

      implicit val request = FakeRequest().withFormUrlEncodedBody(("email.main", ""), ("email.confirm", ""))
      val page = controller.submitPrefsForm(validToken, encodedReturnUrl)(request)

      status(page) shouldBe 400
      contentAsString(page) should include("Enter a valid email address")
      verify(controller.preferencesConnector, times(0)).savePreferencesUnsecured(any[SaUtr], any[Boolean], any[Option[String]])
    }

    "show an error if the confirmed email is not the same as the main" in new WithApplication(FakeApplication()) {
      val controller = createController

      implicit val request = FakeRequest().withFormUrlEncodedBody(("email.main", "valid@mail.com"), ("email.confirm", "notMatching@mail.com"))
      val page = controller.submitPrefsForm(validToken, encodedReturnUrl)(request)

      status(page) shouldBe 400
      val html = Jsoup.parse(contentAsString(page))
      val error = html.getElementsByClass("error-notification")
      error.size() should be (1)
      verify(controller.preferencesConnector, times(0)).savePreferencesUnsecured(any[SaUtr], any[Boolean], any[Option[String]])
    }

    "save the user preferences" in new WithApplication(FakeApplication()) {

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

      val controller = createController
      when(controller.emailConnector.validateEmailAddress(meq(emailAddress))).thenReturn(Future.successful(true))
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(SaPreference(true, Some(SaEmailPreference(emailAddress, Status.verified))))))

      implicit val request = FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress))
      val action = controller.submitPrefsForm(validToken, encodedReturnUrl)(request)

      status(action) shouldBe 303

      verify(controller.emailConnector).validateEmailAddress(meq(emailAddress))
      verify(controller.preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
      verify(controller.preferencesConnector, times(0)).savePreferencesUnsecured(any[SaUtr], any[Boolean], any[Option[String]])

      header("Location", action).get should include("/sa/print-preferences-no-action")
      header("Location", action).get should include("digital=true")
    }

    "redirect to no-action page if the preference is already set to paper when submitting the form" in new WithApplication(FakeApplication()) {

      val controller = createController
      when(controller.emailConnector.validateEmailAddress(meq(emailAddress))).thenReturn(Future.successful(true))
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(SaPreference(false, None))))

      implicit val request = FakeRequest().withFormUrlEncodedBody(("email.main", emailAddress), ("email.confirm", emailAddress))
      val action = controller.submitPrefsForm(validToken, encodedReturnUrl)(request)

      status(action) shouldBe 303
      status(action) shouldBe 303

      verify(controller.emailConnector).validateEmailAddress(meq(emailAddress))
      verify(controller.preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
      verify(controller.preferencesConnector, times(0)).savePreferencesUnsecured(any[SaUtr], any[Boolean], any[Option[String]])

      header("Location", action).get should include("/sa/print-preferences-no-action")
      header("Location", action).get should include("digital=false")
    }

    "return bad request if redirect_url is not in the whitelist" in {
      val controller = createController
      val page = controller.submitPrefsForm(validToken, encodedUrlNotOnWhitelist)(request)
      status(page) shouldBe 400
    }

    "keep paper notification and redirect to the portal" in new WithApplication(FakeApplication()) {
      val controller = createController
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(None))
      when(controller.preferencesConnector.savePreferencesUnsecured(meq(validUtr), meq(false), meq(null))(any())).thenReturn(Future.successful(Some(FormattedUri(new URI("http://1234/")))))

      val page = controller.submitKeepPaperForm(validToken, encodedReturnUrl)(request)

      status(page) shouldBe 303
      header("Location", page).get should equal(decodedReturnUrl)
    }

    "save the user preference to keep the paper notification" in new WithApplication(FakeApplication()) {
      val controller = createController
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(None))
      when(controller.preferencesConnector.savePreferencesUnsecured(meq(validUtr), meq(false), meq(null))(any())).thenReturn(Future.successful(Some(FormattedUri(new URI("http://1234/")))))

      val result = controller.submitKeepPaperForm(validToken, encodedReturnUrl)(request)

      status(result) shouldBe 303

      verify(controller.preferencesConnector, times(1)).savePreferencesUnsecured(meq(validUtr), meq(false), meq(null))
    }

    "redirect to return url if the token is expired when the keep paper notification form is used" in new WithApplication(FakeApplication()) {
      val controller = createController

      val page = controller.submitKeepPaperForm(expiredToken, encodedReturnUrl)(request)

      status(page) shouldBe 303

      verify(controller.preferencesConnector, times(0)).savePreferencesUnsecured(any[SaUtr], any[Boolean], any[Option[String]])
      header("Location", page).get should equal(decodedReturnUrl)
    }

    "redirect to no-action page if the preference is already set to digital when the keep paper notification form is used" in new WithApplication(FakeApplication()) {
      val controller = createController

      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(SaPreference(true, Some(SaEmailPreference(emailAddress, Status.verified))))))

      val action = controller.submitKeepPaperForm(validToken, encodedReturnUrl)(request)

      status(action) shouldBe 303

      verify(controller.preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
      verify(controller.preferencesConnector, times(0)).savePreferencesUnsecured(any[SaUtr], any[Boolean], any[Option[String]])

      header("Location", action).get should include("/sa/print-preferences-no-action")
      header("Location", action).get should include("digital=true")
    }

    "redirect to no-action page if the preference is already set to paper when the keep paper notification form is used" in new WithApplication(FakeApplication()) {
      val controller = createController

      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(SaPreference(false, None))))

      val action = controller.submitKeepPaperForm(validToken, encodedReturnUrl)(request)

      status(action) shouldBe 303

      verify(controller.preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
      verify(controller.preferencesConnector, times(0)).savePreferencesUnsecured(any[SaUtr], any[Boolean], any[Option[String]])

      header("Location", action).get should include("/sa/print-preferences-no-action")
      header("Location", action).get should include("digital=false")
    }
  }

  "The confirm preferences set page" should {
    "reject an invalid return url" in {
      val controller = createController
      val result = controller.confirm(validToken, encodedUrlNotOnWhitelist)(request)
      status(result) should be(400)
    }
    "contain a link with the return url" in new WithApplication(FakeApplication()) {
      val controller = createController
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(
        Future.successful(Some(SaPreference(true, Some(SaEmailPreference(emailAddress, Status.pending)))))
      )
      val result = controller.confirm(validToken, encodedReturnUrl)(request)
      status(result) should be(200)

      val page = Jsoup.parse(contentAsString(result))
      val returnUrl = page.getElementById("sa-home-link").attr("href")
      returnUrl should be (decodedReturnUrlWithEmailAddress)
    }
    "generate an error if the user does not have an email address set" in {
      val controller = createController
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(
        Future.successful(Some(SaPreference(true, None)))
      )
      val result = controller.confirm(validToken, encodedReturnUrl)(request)
      status(result) should be(412)
    }
    "handle return urls which do not already have query parameters" in new WithApplication(FakeApplication()) {
      val urlWithQueryParams = "http://localhost:8080/portal"
      val encodedUrlWithQueryParams = urlEncode(urlWithQueryParams, "UTF-8")

      val controller = createController
      when(controller.preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(
        Future.successful(Some(SaPreference(true, Some(SaEmailPreference(emailAddress, Status.pending)))))
      )
      val result = controller.confirm(validToken, encodedUrlWithQueryParams)(request)
      status(result) should be(200)

      val page = Jsoup.parse(contentAsString(result))
      val returnUrl = page.getElementById("sa-home-link").attr("href")
      returnUrl should be (s"$urlWithQueryParams?emailAddress=${urlEncode(encrypt(emailAddress), "UTF-8")}")
    }
  }
}