package controllers.external

import connectors._
import helpers.ConfigHelper
import org.jsoup.Jsoup
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class EmailValidationSpec extends WordSpec with ShouldMatchers with MockitoSugar with OneAppPerSuite {

  def createController = new EmailValidationController {
    override lazy val entityResolverConnector = mock[EntityResolverConnector]
  }

  val wellFormattedToken: String = "12345678-abcd-4abc-abcd-123456789012"
  val tokenWithSomeExtraStuff: String = "12345678-abcd-4abc-abcd-123456789012423"

  implicit def hc = any[HeaderCarrier]
  implicit val request = FakeRequest()

  override implicit lazy val app : Application = ConfigHelper.fakeApp

  "verify" should {
    "call the sa micro service and update the email verification status of the user" in {
      val controller = createController
      val token = wellFormattedToken
      when(controller.entityResolverConnector.updateEmailValidationStatusUnsecured(meq(token))).thenReturn(Future.successful(Validated))

      val response = controller.verify(token)(request)

      contentAsString(response) shouldNot include("portalHomeLink/home")
      status(response) shouldBe 200
    }

    "call the sa micro service and update the email verification status of the user when supplied a return url and return link text" in {
      val controller = createController
      val token = wellFormattedToken
      when(controller.entityResolverConnector.updateEmailValidationStatusUnsecured(meq(token))).thenReturn(Future.successful(ValidatedWithReturn("Return link text", "/ReturnUrl")))

      val response = controller.verify(token)(request)

      contentAsString(response) should include("Return link text")
      status(response) shouldBe 200
    }

    "display an error when the sa micro service fails to update a users email verification status" in {
      val controller = createController
      val token = wellFormattedToken
      when(controller.entityResolverConnector.updateEmailValidationStatusUnsecured(meq(token))).thenReturn(Future.successful(ValidationError))
      val response = controller.verify(token)(request)
      contentAsString(response) shouldNot include("portalHomeLink/home")
      status(response) shouldBe 400
    }

    "display an error if the email verification token is out of date" in {
      val controller = createController
      val token = wellFormattedToken
      when(controller.entityResolverConnector.updateEmailValidationStatusUnsecured(meq(token))).thenReturn(Future.successful(ValidationExpired))

      val response = controller.verify(token)(request)

      status(response) shouldBe 200
      val html = contentAsString(response)
      html shouldNot include("portalHomeLink/home")
      val page = Jsoup.parse(html)
      page.getElementsByTag("h1").first.text shouldBe "This link has expired"
    }

    "display an error if the email verification token is not for the email pending verification" in {
      val controller = createController
      val token = wellFormattedToken
      when(controller.entityResolverConnector.updateEmailValidationStatusUnsecured(meq(token))).thenReturn(Future.successful(WrongToken))

      val response = controller.verify(token)(request)

      status(response) shouldBe 200
      val html = contentAsString(response)
      html shouldNot include("portalHomeLink/home")
      val page = Jsoup.parse(html)
      page.getElementsByTag("h1").first.text shouldBe "You've used a link that has now expired"
    }

    "display an error if the token is not in a valid uuid format without calling the service" in {
      val controller = createController
      val token = "badToken"
      val response = controller.verify(token)(request)
      contentAsString(response) shouldNot include("portalHomeLink/home")
      status(response) shouldBe 400
      verify(controller.entityResolverConnector, never()).updateEmailValidationStatusUnsecured(meq(token))
    }

    "display an error if the token is not in a valid uuid format (extra characters) without calling the service" in {
      val controller = createController
      val token = tokenWithSomeExtraStuff
      val response = controller.verify(token)(request)
      contentAsString(response) shouldNot include("portalHomeLink/home")
      status(response) shouldBe 400
      verify(controller.entityResolverConnector, never()).updateEmailValidationStatusUnsecured(meq(token))
    }

  }
}