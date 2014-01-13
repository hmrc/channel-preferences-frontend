package controllers

import org.scalatest._
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.PreferencesConnector
import org.mockito.Mockito._
import play.api.test.Helpers._
import play.api.test.{FakeApplication, WithApplication, FakeRequest}
import org.jsoup.Jsoup
import scala.concurrent.Future
import controllers.sa.prefs.EmailValidation

class EmailValidationSpec extends WordSpec with ShouldMatchers with MockitoSugar {

  import uk.gov.hmrc.EmailVerificationLinkResponse._

  val additionalConfig = Map("sa-prefs.Test.portal.destinationRoot" -> "portalHomeLink", "sa-prefs.Test.portal.destinationPath.home" -> "/home")

  def createController = new EmailValidation {
    override lazy val preferencesMicroService = mock[PreferencesConnector]
  }

  val wellFormattedToken: String = "12345678-abcd-4abc-abcd-123456789012"
  val tokenWithSomeExtraStuff: String = "12345678-abcd-4abc-abcd-123456789012423"

  "verify" should {

    "call the sa micro service and update the email verification status of the user" in new WithApplication(FakeApplication(additionalConfiguration = additionalConfig)) {
      val controller = createController
      val token = wellFormattedToken
      when(controller.preferencesMicroService.updateEmailValidationStatus(token)).thenReturn(Future.successful(OK))
      val response = controller.verify(token)(FakeRequest())
      contentAsString(response) shouldNot include("portalHomeLink/home")
      status(response) shouldBe 200
      verify(controller.preferencesMicroService).updateEmailValidationStatus(token)
    }

    "display an error when the sa micro service fails to update a users email verification status" in new WithApplication(FakeApplication(additionalConfiguration = additionalConfig)) {
      val controller = createController
      val token = wellFormattedToken
      when(controller.preferencesMicroService.updateEmailValidationStatus(token)).thenReturn(Future.successful(ERROR))
      val response = controller.verify(token)(FakeRequest())
      contentAsString(response) shouldNot include("portalHomeLink/home")
      status(response) shouldBe 400
      verify(controller.preferencesMicroService).updateEmailValidationStatus(token)
    }

    "display an error if the email verification token is out of date" in new WithApplication(FakeApplication(additionalConfiguration =  additionalConfig)) {
      val controller = createController
      val token = wellFormattedToken
      when(controller.preferencesMicroService.updateEmailValidationStatus(token)).thenReturn(Future.successful(EXPIRED))

      val response = controller.verify(token)(FakeRequest())

      status(response) shouldBe 200
      val html = contentAsString(response)
      html shouldNot include("portalHomeLink/home")
      val page = Jsoup.parse(html)
      page.getElementsByTag("h2").first.text shouldBe "Looks like we have a problem..."
      page.getElementsByTag("p").text should include("out of date")
      verify(controller.preferencesMicroService).updateEmailValidationStatus(token)
    }

    "display an error if the token is not in a valid uuid format without calling the service" in {
      val controller = createController
      val token = "badToken"
      when(controller.preferencesMicroService.updateEmailValidationStatus(token)).thenReturn(Future.successful(ERROR))
      val response = controller.verify(token)(FakeRequest())
      contentAsString(response) shouldNot include("portalHomeLink/home")
      status(response) shouldBe 400
      verify(controller.preferencesMicroService, never()).updateEmailValidationStatus(token)
    }

    "display an error if the token is not in a valid uuid format (extra characters) without calling the service" in {
      val controller = createController
      val token = tokenWithSomeExtraStuff
      when(controller.preferencesMicroService.updateEmailValidationStatus(token)).thenReturn(Future.successful(ERROR))
      val response = controller.verify(token)(FakeRequest())
      contentAsString(response) shouldNot include("portalHomeLink/home")
      status(response) shouldBe 400
      verify(controller.preferencesMicroService, never()).updateEmailValidationStatus(token)
    }

  }
}