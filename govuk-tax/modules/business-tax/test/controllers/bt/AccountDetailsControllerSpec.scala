package controllers.bt

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeRequest, FakeApplication, WithApplication}
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import scala.concurrent.Future
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.preferences.{SaPreference, PreferencesConnector}
import org.mockito.Mockito._
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.email.EmailConnector

abstract class Setup extends WithApplication(FakeApplication()) with MockitoSugar {
  val auditConnector = mock[AuditConnector]
  val authConnector = mock[AuthConnector]
  val mockPreferencesConnector = mock[PreferencesConnector]
  val mockEmailConnector = mock[EmailConnector]
  val controller = new AccountDetailsController(auditConnector, mockPreferencesConnector,mockEmailConnector)(authConnector)

  val request = FakeRequest()
}

class AccountDetailsControllerSpec extends BaseSpec with MockitoSugar  {

  import play.api.test.Helpers._

  val validUtr = SaUtr("1234567890")
  val saRoot = Some(SaRoot(validUtr, Map.empty[String, String]))
  val user = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(sa = saRoot), decryptedToken = None)

  "show account details" should {

    "include Change email address section for SA customer with set email preference" in new Setup {

      val saPreferences = SaPreference(true, Some("test@test.com"))
      when(mockPreferencesConnector.getPreferences(validUtr)(HeaderCarrier())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller.accountDetailsPage(user, request))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))
      page.getElementById("changeEmailAddressLink").text shouldBe "Change your email address"
      page.getElementById("changeEmailAddressLink").attr("href") shouldBe routes.AccountDetailsController.changeEmailAddress().url

      verify(mockPreferencesConnector).getPreferences(validUtr)

    }

    "not include Change email address section for non-SA customer" in new Setup {

      val nonSaUser = User(userId = "userId", userAuthority = UserAuthority("userId", Regimes()), nameFromGovernmentGateway = Some("Ciccio"), regimes = RegimeRoots(), decryptedToken = None)

      val result = Future.successful(controller.accountDetailsPage(nonSaUser, request))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))
      page.getElementById("changeEmailAddressLink") shouldBe null

      verifyZeroInteractions(mockPreferencesConnector)
    }

    "not include Change email address section for a SA customer who has not yet set his email preference" in new Setup {

      when(mockPreferencesConnector.getPreferences(validUtr)(HeaderCarrier())).thenReturn(Future.successful(None))

      val result = Future.successful(controller.accountDetailsPage(user, request))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))
      page.getElementById("changeEmailAddressLink") shouldBe null

      verify(mockPreferencesConnector).getPreferences(validUtr)
    }

    "not include Change email address section for a SA customer who has opted into paper notification" in new Setup {

      val saPreferences = SaPreference(false, None)
      when(mockPreferencesConnector.getPreferences(validUtr)(HeaderCarrier())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller.accountDetailsPage(user, request))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))
      page.getElementById("changeEmailAddressLink") shouldBe null

      verify(mockPreferencesConnector).getPreferences(validUtr)
    }
  }

  "clicking on Change email address link in the account details page" should {
    "display update email address form when accessed from Account Details" in new Setup {

      val saPreferences = SaPreference(true, Some("test@test.com"))
      when(mockPreferencesConnector.getPreferences(validUtr)(HeaderCarrier())).thenReturn(Future.successful(Some(saPreferences)))

      val result = Future.successful(controller.changeEmailAddressPage(user, request))

      status(result) shouldBe 200
      val page = Jsoup.parse(contentAsString(result))

      page.getElementById("currentEmailAddress").text shouldBe "test@test.com"
      page.getElementById("email") shouldNot be(null)
      page.getElementById("email").attr("value") shouldBe ""
      //page.getElementById("verifyEmail") shouldNot be(null)

    }

    "display update email address form with the email input field pre-populated when coming back from the warning page" in {
      pending
    }
  }

  "update email address" should {

    "validate the email address, update the address for SA user and redirect to confirmation page" in {
      pending
    }

    "show error if the 2 email address fields don't match" in {
      pending
    }

    "show error if the email address is not syntactically valid" in {
//      val emailAddress = "invalid-email"
//
//      val page = Future.successful(controller.submitFormAction(user, FakeRequest().withFormUrlEncodedBody(("email", emailAddress))))
//
//      status(page) shouldBe 400
//
//      val document = Jsoup.parse(contentAsString(page))
//      document.select(".error-notification").text shouldBe "Please provide a valid email address"
//      verifyZeroInteractions(preferencesConnector, emailConnector)
      pending
    }

    "show error if the email field is empty" in {
      pending
    }

    "show a warning page if the email has a valid structure but does not pass validation by the email micro service" in {
      pending
    }

  }

}
