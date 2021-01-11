/*
 * Copyright 2021 HM Revenue & Customs
 *
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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

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
          Matchers.any())).thenReturn(Future.successful(true))

      connector.isValid(exampleEmailAddress).futureValue mustBe true
    }

    "return false if the service returns false" in {
      when(
        mockHttpClient.POST[UpdateEmail, Boolean](Matchers.any(), Matchers.any(), Matchers.any())(
          Matchers.any(),
          Matchers.any(),
          Matchers.any(),
          Matchers.any())).thenReturn(Future.successful(false))
      connector.isValid(exampleEmailAddress).futureValue mustBe false
    }

    "returns false if service unavailable" in {
      when(
        mockHttpClient.POST[UpdateEmail, Boolean](Matchers.any(), Matchers.any(), Matchers.any())(
          Matchers.any(),
          Matchers.any(),
          Matchers.any(),
          Matchers.any())).thenReturn(Future.failed(new Exception("service down")))
      connector.isValid(exampleEmailAddress).futureValue mustBe false
    }
  }
}
