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

package connectors

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient }

import scala.concurrent.Future

class EmailConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite {

  implicit val hc = HeaderCarrier()
  val exampleEmailAddress = "bob@somewhere.com"
  val mockHttpClient = mock[HttpClient]

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[HttpClient].toInstance(mockHttpClient)
      )
      .build()

  def connector = app.injector.instanceOf[EmailConnector]

  "Validating an email address" should {

    "return true if the service returns true" in {
      when(
        mockHttpClient.POST[UpdateEmail, Boolean](Matchers.any(), Matchers.any(), Matchers.any())(
          Matchers.any(),
          Matchers.any(),
          Matchers.any(),
          Matchers.any()
        )
      ).thenReturn(Future.successful(true))

      connector.isValid(exampleEmailAddress).futureValue mustBe true
    }

    "return false if the service returns false" in {
      when(
        mockHttpClient.POST[UpdateEmail, Boolean](Matchers.any(), Matchers.any(), Matchers.any())(
          Matchers.any(),
          Matchers.any(),
          Matchers.any(),
          Matchers.any()
        )
      ).thenReturn(Future.successful(false))
      connector.isValid(exampleEmailAddress).futureValue mustBe false
    }

    "returns false if service unavailable" in {
      when(
        mockHttpClient.POST[UpdateEmail, Boolean](Matchers.any(), Matchers.any(), Matchers.any())(
          Matchers.any(),
          Matchers.any(),
          Matchers.any(),
          Matchers.any()
        )
      ).thenReturn(Future.failed(new Exception("service down")))
      connector.isValid(exampleEmailAddress).futureValue mustBe false
    }
  }
}
