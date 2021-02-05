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

package helpers

import controllers.MetricOrchestratorStub
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.metrix.MetricOrchestrator
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.bootstrap.config.AuditingConfigProvider
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpAuditing

trait ConfigHelper extends MetricOrchestratorStub {
  self: GuiceOneAppPerSuite =>

  private def additionalConfig =
    Map(
      "govuk-tax.Test.services.contact-frontend.host" -> "localhost",
      "govuk-tax.Test.services.contact-frontend.port" -> "9250",
      "govuk-tax.Test.assets.url"                     -> "fake/url",
      "govuk-tax.Test.assets.version"                 -> "54321",
      "application.langs"                             -> "en,cy",
      "govuk-tax.Test.google-analytics.host"          -> "host",
      "govuk-tax.Test.google-analytics.token"         -> "aToken"
    )

  lazy val fakeApp = new GuiceApplicationBuilder()
    .overrides(bind[MetricOrchestrator].toInstance(mockMetricOrchestrator).eagerly())
    .overrides(bind[AuditingConfig].toProvider[AuditingConfigProvider].eagerly())
    .overrides(bind[HttpAuditing].to[DefaultHttpAuditing].eagerly())
    .configure(additionalConfig)
    .configure("metrics.enabled" -> false)
    .build()

}
