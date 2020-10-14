/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package config

import controllers.ExternalUrls
import javax.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.RunMode

trait AppConfig {
  val assetsPrefix: String
  val analyticsToken: String
  val analyticsHost: String
  val betaFeedbackUrl: String
  val betaFeedbackUnauthenticatedUrl: String
  val homeUrl: String
  val accountDetailsUrl: String
  val helpUrl: String
  val signOutUrl: String
}

class YtaConfig @Inject()(configuration: Configuration, runMode: RunMode, externalUrls: ExternalUrls)
    extends AppConfig {

  private def loadConfig(key: String) =
    configuration.getOptional[String](key).getOrElse(throw new RuntimeException(s"Missing key: $key"))

  override lazy val assetsPrefix = externalUrls.assets
  override lazy val analyticsToken = loadConfig(s"govuk-tax.${runMode.env}.google-analytics.token")
  override lazy val analyticsHost = loadConfig(s"govuk-tax.${runMode.env}.google-analytics.host")
  override lazy val betaFeedbackUrl = externalUrls.betaFeedbackUrl
  override lazy val betaFeedbackUnauthenticatedUrl = externalUrls.betaFeedbackUnauthenticatedUrl
  override lazy val homeUrl = externalUrls.businessTaxHome
  override lazy val accountDetailsUrl = externalUrls.accountDetails
  override lazy val helpUrl = externalUrls.helpUrl
  override lazy val signOutUrl = externalUrls.survey

  def fallbackURLForLanguageSwitcher: String = loadConfig(s"${runMode.env}.languageSwitcher.fallback.url")
  def enableLanguageSwitching: Boolean =
    configuration.getOptional[Boolean](s"${runMode.env}.enableLanguageSwitching").getOrElse(false)
  def surveyReOptInPage10Enabled: Boolean =
    configuration.getOptional[Boolean](s"survey.ReOptInPage10.enabled").getOrElse(false)
}
