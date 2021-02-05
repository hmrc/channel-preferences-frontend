/*
 * Copyright 2021 HM Revenue & Customs
 *
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
