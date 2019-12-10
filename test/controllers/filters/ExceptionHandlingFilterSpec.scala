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

package controllers.filters

import java.util.concurrent.TimeUnit.SECONDS

import akka.util.Timeout
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{ FakeRequest, Helpers }
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException

import scala.concurrent.Future

class ExceptionHandlingFilterSpec
    extends WordSpec with Matchers with MockitoSugar with GuiceOneAppPerSuite with ScalaFutures {

  implicit val timeout = Timeout(5, SECONDS)
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .build()
  val exceptionHandlingFilter = app.injector.instanceOf[ExceptionHandlingFilter]

  "ExceptionHandlingFilter" should {

    "redirect to the returnUrl if there is an exception handling the request" in {
      val returnUrl = "Wa6yuBSzGvUaibkXblJ8aQ%3D%3D"
      implicit val queryStringBindable = model.Encrypted.encryptedStringToDecryptedString

      val fakeRequest = FakeRequest("GET", s"testUrl?returnUrl=$returnUrl")

      val filterResult = exceptionHandlingFilter(_ => Future.failed(new RuntimeException))(fakeRequest)

      Helpers.redirectLocation(filterResult) shouldBe Some("foo&value")
    }

    "return UnhealthyServiceException if there is a UnhealthyServiceException exception even if there is a returnUrl in the request" in {
      val returnUrl = "Wa6yuBSzGvUaibkXblJ8aQ%3D%3D"
      val fakeRequest = FakeRequest("GET", s"testUrl?returnUrl=$returnUrl")

      implicit val queryStringBindable = model.Encrypted.encryptedStringToDecryptedString
      val actionException = new UnhealthyServiceException("She kanna take any more captain!")

      val filterResult = exceptionHandlingFilter(_ => Future.failed(actionException))(fakeRequest)

      filterResult.failed.futureValue shouldBe actionException
    }

    "return 500 if there is an exception but no returnUrl in the request" in {
      val fakeRequest = FakeRequest("GET", s"testUrl")
      implicit val queryStringBindable = model.Encrypted.encryptedStringToDecryptedString
      val actionException = new RuntimeException

      val filterResult = exceptionHandlingFilter(_ => Future.failed(actionException))(fakeRequest)

      filterResult.failed.futureValue shouldBe actionException
    }
  }
}
