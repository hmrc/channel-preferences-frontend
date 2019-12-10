/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers

import controllers.auth.{ AuthenticatedRequest, WithAuthRetrievals }
import org.joda.time.{ DateTime, DateTimeZone }
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{ LoginTimes, Name, ~ }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ ExecutionContext, Future }

class AuthControllerSpecs extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite {

  val fakeRequest = FakeRequest("GET", "/")

  type AuthRetrievals = Option[Name] ~ LoginTimes ~ Option[String] ~ Option[String]

  val currentLogin = new DateTime(2015, 1, 1, 12, 0).withZone(DateTimeZone.UTC)
  val previousLogin = new DateTime(2012, 1, 1, 12, 0).withZone(DateTimeZone.UTC)

  val retrievalResult: Future[Option[Name] ~ LoginTimes ~ Option[String] ~ Option[String]] =
    Future.successful(
      new ~(
        new ~(
          new ~(Some(Name(Some("Alex"), Some("Brown"))), LoginTimes(currentLogin, Some(previousLogin))),
          Option.empty[String]),
        Some("1234567890")
      ))

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].toInstance(mockAuthConnector)
      )
      .build()
  class FakeController(val authConnector: AuthConnector, mcc: MessagesControllerComponents)(
    implicit ec: ExecutionContext)
      extends FrontendController(mcc) with WithAuthRetrievals {
    def onPageLoad() = Action.async { implicit request =>
      withAuthenticatedRequest { authenticatedRequest: AuthenticatedRequest[_] => hc: HeaderCarrier =>
        Future.successful(Ok)
      }
    }
  }
  val mcc = app.injector.instanceOf[MessagesControllerComponents]
  implicit val ec = app.injector.instanceOf[ExecutionContext]
  val controller = new FakeController(mockAuthConnector, mcc)

  "Auth Action" when {
    "the user has authenticated should return a successful response" in {
      when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any()))
        .thenReturn(retrievalResult)
      val result = controller.onPageLoad()(fakeRequest)
      status(result) mustBe OK
    }

    "return not authorised then no credentials supplied" in {
      when(mockAuthConnector.authorise[AuthRetrievals](any(), any())(any(), any()))
        .thenReturn(Future.failed(SessionRecordNotFound()))
      val result = controller.onPageLoad()(fakeRequest)
      status(result) mustBe 401
    }

  }
}
