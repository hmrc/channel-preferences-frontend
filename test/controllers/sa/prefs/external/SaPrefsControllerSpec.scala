package controllers.sa.prefs.external

import org.scalatest.{OptionValues, BeforeAndAfter, ShouldMatchers, WordSpec}
import play.api.test.WithApplication
import play.api.test.FakeRequest
import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.joda.time.{DateTimeZone, DateTime}
import java.net.URLEncoder.{encode => urlEncode}
import org.jsoup.Jsoup
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import scala.concurrent.Future
import connectors.EmailConnector
import org.scalatest.concurrent.ScalaFutures
import scala.Some
import uk.gov.hmrc.domain.SaUtr
import play.api.test.FakeApplication
import java.net.URI
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.common.crypto.Encrypted
import uk.gov.hmrc.play.connectors.HeaderCarrier
import connectors.{FormattedUri, PreferencesConnector, SaEmailPreference, SaPreference}
import connectors.SaEmailPreference.Status
import uk.gov.hmrc.common.crypto.ApplicationCrypto.SsoPayloadCrypto.encrypt

class SaPrefsControllerSpec extends WordSpec with ShouldMatchers with MockitoSugar with BeforeAndAfter with ScalaFutures with OptionValues {

  import play.api.test.Helpers._

  "Preferences pages" should {
    "redirect to the portal when no preference exists for a specific utr" in new SaPrefsControllerApp {
      when(preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(None))

      val page = controller.index(validToken, encodedReturnUrl, None)(FakeRequest())
      status(page) shouldBe 303
      header("Location", page).value should be (decodedReturnUrl)
      verify(preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
    }

    "redirect to the portal when a preference for email already exists for a specific utr" in new SaPrefsControllerApp {
      val preferencesAlreadyCreated = SaPreference(true, Some(SaEmailPreference(emailAddress, status = Status.verified)))
      when(preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(preferencesAlreadyCreated)))

      val page = controller.index(validToken, encodedReturnUrl, None)(FakeRequest())
      status(page) shouldBe 303
      header("Location", page).value should be (decodedReturnUrlWithEmailAddress)
      verify(preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
    }

    "redirect to the portal when a preference for paper already exists for a specific utr" in new SaPrefsControllerApp {
      
      val preferencesAlreadyCreated = SaPreference(false, None)
      when(preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(preferencesAlreadyCreated)))

      val page = controller.index(validToken, encodedReturnUrl, None)(FakeRequest())
      status(page) shouldBe 303
      header("Location", page).value should be (decodedReturnUrl)
      verify(preferencesConnector, times(1)).getPreferencesUnsecured(meq(validUtr))
    }

    "redirect to the portal when preferences already exist for a specific utr and an email address was passed to the platform" in new SaPrefsControllerApp {
      
      val preferencesAlreadyCreated = SaPreference(true, Some(SaEmailPreference(emailAddress, status = Status.verified)))
      when(preferencesConnector.getPreferencesUnsecured(meq(validUtr))).thenReturn(Future.successful(Some(preferencesAlreadyCreated)))

      val page = controller.index(validToken, encodedReturnUrl, Some(Encrypted(EmailAddress("other@me.com"))))(FakeRequest())
      status(page) shouldBe 303
      header("Location", page).value should be (decodedReturnUrlWithEmailAddress)
    }

    "redirect to portal if the token is expired on the landing page" in new SaPrefsControllerApp {
      val page = controller.index(expiredToken, encodedReturnUrl, None)(FakeRequest())

      status(page) shouldBe 303
      header("Location", page).get should equal(decodedReturnUrl)
    }

    "redirect to portal if the token is not valid on the landing page" in new SaPrefsControllerApp {
      val page = controller.index(incorrectToken, encodedReturnUrl, None)(FakeRequest())

      status(page) shouldBe 303
      header("Location", page).get should equal(decodedReturnUrl)
    }

    "return bad request if redirect_url is not in the whitelist" in new SaPrefsControllerApp {
      
      val page = controller.index(validToken, encodedUrlNotOnWhitelist, None)(FakeRequest())
      status(page) shouldBe 400
    }
  }

}

class SaPrefsControllerApp extends WithApplication(FakeApplication()) with MockitoSugar{

  val preferencesConnector = mock[PreferencesConnector]
  val emailConnector = mock[EmailConnector]
  val controller = new SaPrefsController(whiteList = Set("localhost"), preferencesConnector, emailConnector)

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
        Seq(mainEmail.map{v => "email.main" -> v})
        ++ Seq(mainEmailConfirmation.map{v => ("email.confirm", v)})
        ++ Seq(optIn.map{v => ("opt-in", v.toString)})
      ).flatten

    FakeRequest().withFormUrlEncodedBody(params: _*)

  }

}