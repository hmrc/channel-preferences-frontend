/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.channelpreferencesfrontend.base

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import org.scalatest.TryValues
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.i18n.{ Messages, MessagesApi }
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.CSRFTokenHelper._

@SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.DefaultArguments"))
trait SpecBase extends PlaySpec with TryValues with ScalaFutures with IntegrationPatience {

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(fakeRequest("", ""))

  def fakeRequest(method: String = "", path: String = ""): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, path).withCSRFToken.asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]

  protected def applicationBuilder(): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[Metrics].toInstance(new FakeMetrics)
      )
}

class FakeMetrics extends Metrics {
  override val defaultRegistry: MetricRegistry = new MetricRegistry
  override val toJson: String = "{}"
}
