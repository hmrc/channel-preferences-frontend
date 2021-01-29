/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.external

import connectors._
import org.jsoup.Jsoup
import org.mockito.Matchers.{ any, eq => meq }
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class EmailValidationSpec extends WordSpec with Matchers with MockitoSugar with GuiceOneAppPerSuite {

  val mockEntityResolverConnector = mock[EntityResolverConnector]
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[EntityResolverConnector].toInstance(mockEntityResolverConnector)
      )
      .build()
  def createController = app.injector.instanceOf[EmailValidationController]

  val wellFormattedToken: String = "12345678-abcd-4abc-abcd-123456789012"
  val tokenWithSomeExtraStuff: String = "12345678-abcd-4abc-abcd-123456789012423"

  implicit def hc = any[HeaderCarrier]
  implicit val request = FakeRequest()

  "verify" should {
    "call the sa micro service and update the email verification status of the user" in {
      val controller = createController
      val token = wellFormattedToken
      when(mockEntityResolverConnector.updateEmailValidationStatusUnsecured(meq(token)))
        .thenReturn(Future.successful(Validated))

      val response = controller.verify(token)(request)

      contentAsString(response) shouldNot include("portalHomeLink/home")
      status(response) shouldBe 200
    }

    "call the sa micro service and update the email verification status of the user when supplied a return url and return link text" in {
      val controller = createController
      val token = wellFormattedToken
      when(mockEntityResolverConnector.updateEmailValidationStatusUnsecured(meq(token)))
        .thenReturn(Future.successful(ValidatedWithReturn("Return link text", "/ReturnUrl")))

      val response = controller.verify(token)(request)

      contentAsString(response) should include("Return link text")
      status(response) shouldBe 200
    }

    "call the sa micro service and update the email verification status of the user after it has aready been verified" in {
      val controller = createController
      val token = wellFormattedToken
      when(mockEntityResolverConnector.updateEmailValidationStatusUnsecured(meq(token)))
        .thenReturn(Future.successful(ValidationError))

      val response = controller.verify(token)(request)

      contentAsString(response) shouldNot include("portalHomeLink/home")
      status(response) shouldBe BAD_REQUEST
    }

    "call the sa micro service and update the email verification status of the user after it has aready been verified, when opted in through a token service" in {
      val controller = createController
      val token = wellFormattedToken
      when(mockEntityResolverConnector.updateEmailValidationStatusUnsecured(meq(token)))
        .thenReturn(Future.successful(ValidationErrorWithReturn("return link text", "returnUrl")))

      val response = controller.verify(token)(request)

      contentAsString(response) should include("return link text")
      status(response) shouldBe BAD_REQUEST
    }

    "display an error when the sa micro service fails to update a users email verification status" in {
      val controller = createController
      val token = wellFormattedToken
      when(mockEntityResolverConnector.updateEmailValidationStatusUnsecured(meq(token)))
        .thenReturn(Future.successful(ValidationError))
      val response = controller.verify(token)(request)
      contentAsString(response) shouldNot include("portalHomeLink/home")
      status(response) shouldBe 400
    }

    "display an error if the email verification token is out of date" in {
      val controller = createController
      val token = wellFormattedToken
      when(mockEntityResolverConnector.updateEmailValidationStatusUnsecured(meq(token)))
        .thenReturn(Future.successful(ValidationExpired))

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
      when(mockEntityResolverConnector.updateEmailValidationStatusUnsecured(meq(token)))
        .thenReturn(Future.successful(WrongToken))

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
      verify(mockEntityResolverConnector, never()).updateEmailValidationStatusUnsecured(meq(token))
    }

    "display an error if the token is not in a valid uuid format (extra characters) without calling the service" in {
      val controller = createController
      val token = tokenWithSomeExtraStuff
      val response = controller.verify(token)(request)
      contentAsString(response) shouldNot include("portalHomeLink/home")
      status(response) shouldBe 400
      verify(mockEntityResolverConnector, never()).updateEmailValidationStatusUnsecured(meq(token))
    }

  }
}
