package controllers.sa.prefs.filing

import java.net.URLEncoder.{encode => urlEncode}

import connectors.SaEmailPreference.Status
import connectors.{EmailConnector, PreferencesConnector, SaEmailPreference, SaPreference}
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, OptionValues, ShouldMatchers, WordSpec}
import play.api.mvc.{AnyContent, Request}
import play.api.test.{FakeApplication, FakeRequest, WithApplication}
import uk.gov.hmrc.common.crypto.ApplicationCrypto.SsoPayloadCrypto.encrypt
import uk.gov.hmrc.common.crypto.Encrypted
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.connectors.HeaderCarrier
import uk.gov.hmrc.play.test.WithFakeApplication

import scala.concurrent.Future

class FilingInterceptControllerSpec extends WordSpec with ShouldMatchers with MockitoSugar with BeforeAndAfter with ScalaFutures with OptionValues with WithFakeApplication {

  import play.api.test.Helpers._

  "Preferences pages" should {
    "redirect to the portal when no preference exists for a specific utr" in new TestCase {
      when(preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(None))

      val page = controller.redirectWithEmailAddress(validToken, encodedReturnUrl, None)(FakeRequest())
      status(page) shouldBe 303
      header("Location", page).value should be(decodedReturnUrl)
      verify(preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
    }

    "redirect to the portal when a preference for email already exists for a specific utr" in new TestCase {
      val preferencesAlreadyCreated = SaPreference(true, Some(SaEmailPreference(emailAddress, status = Status.verified)))
      when(preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(preferencesAlreadyCreated)))

      val page = controller.redirectWithEmailAddress(validToken, encodedReturnUrl, None)(FakeRequest())
      status(page) shouldBe 303
      header("Location", page).value should be(decodedReturnUrlWithEmailAddress)
      verify(preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
    }

    "redirect to the portal when a preference for paper already exists for a specific utr" in new TestCase {

      val preferencesAlreadyCreated = SaPreference(false, None)
      when(preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(preferencesAlreadyCreated)))

      val page = controller.redirectWithEmailAddress(validToken, encodedReturnUrl, None)(FakeRequest())
      status(page) shouldBe 303
      header("Location", page).value should be(decodedReturnUrl)
      verify(preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
    }

    "redirect to the portal when preferences already exist for a specific utr and an email address was passed to the platform" in new TestCase {

      val preferencesAlreadyCreated = SaPreference(true, Some(SaEmailPreference(emailAddress, status = Status.verified)))
      when(preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(preferencesAlreadyCreated)))

      val page = controller.redirectWithEmailAddress(validToken, encodedReturnUrl, Some(Encrypted(EmailAddress("other@me.com"))))(FakeRequest())
      status(page) shouldBe 303
      header("Location", page).value should be(decodedReturnUrlWithEmailAddress)
    }

    "redirect to portal if the token is expired on the landing page" in new TestCase {
      val page = controller.redirectWithEmailAddress(expiredToken, encodedReturnUrl, None)(FakeRequest())

      status(page) shouldBe 303
      header("Location", page).get should equal(decodedReturnUrl)
    }

    "redirect to portal if the token is not valid on the landing page" in new TestCase {
      val page = controller.redirectWithEmailAddress(incorrectToken, encodedReturnUrl, None)(FakeRequest())

      status(page) shouldBe 303
      header("Location", page).get should equal(decodedReturnUrl)
    }

    "return bad request if redirect_url is not in the whitelist" in new TestCase {

      val page = controller.redirectWithEmailAddress(validToken, encodedUrlNotOnWhitelist, None)(FakeRequest())
      status(page) shouldBe 400
    }
  }

  trait TestCase {

    val preferencesConnector = mock[PreferencesConnector]
    val emailConnector = mock[EmailConnector]
    val controller = new FilingInterceptController(whiteList = Set("localhost"), preferencesConnector, emailConnector)

    val emailAddress = "foo@bar.com"
    val validUtr = SaUtr("1234567")
    lazy val validToken = urlEncode(encrypt(s"$validUtr:${DateTime.now(DateTimeZone.UTC).getMillis}"), "UTF-8")
    lazy val expiredToken = urlEncode(encrypt(s"$validUtr:${DateTime.now(DateTimeZone.UTC).minusDays(1).getMillis}"), "UTF-8")
    lazy val incorrectToken = "this is an incorrect token khdskjfhasduiy3784y37yriuuiyr3i7rurkfdsfhjkdskh"
    val decodedReturnUrl = "http://localhost:8080/portal?exampleQuery=exampleValue"
    val encodedReturnUrl = urlEncode(decodedReturnUrl, "UTF-8")
    lazy val decodedReturnUrlWithEmailAddress = s"$decodedReturnUrl&email=${urlEncode(encrypt(emailAddress), "UTF-8")}"
    val encodedUrlNotOnWhitelist = urlEncode("http://notOnWhiteList/something", "UTF-8")

    val request = FakeRequest()

    implicit def hc: HeaderCarrier = any()

    def request(optIn: Option[Boolean], mainEmail: Option[String] = None, mainEmailConfirmation: Option[String] = None): Request[AnyContent] = {

      val params = (
        Seq(mainEmail.map { v => "email.main" -> v})
          ++ Seq(mainEmailConfirmation.map { v => ("email.confirm", v)})
          ++ Seq(optIn.map { v => ("opt-in", v.toString)})
        ).flatten

      FakeRequest().withFormUrlEncodedBody(params: _*)

    }

  }

}