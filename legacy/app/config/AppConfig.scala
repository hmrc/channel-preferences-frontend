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

package config

import controllers.ExternalUrls
import play.api.{ Configuration, Environment }

import javax.inject.Inject

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

class YtaConfig @Inject() (configuration: Configuration, env: Environment, externalUrls: ExternalUrls)
    extends AppConfig {

  private def loadConfig(key: String) =
    configuration.getOptional[String](key).getOrElse(throw new RuntimeException(s"Missing key: $key"))

  override lazy val assetsPrefix = externalUrls.assets
  override lazy val analyticsToken = loadConfig(s"govuk-tax.${env.mode}.google-analytics.token")
  override lazy val analyticsHost = loadConfig(s"govuk-tax.${env.mode}.google-analytics.host")
  override lazy val betaFeedbackUrl = externalUrls.betaFeedbackUrl
  override lazy val betaFeedbackUnauthenticatedUrl = externalUrls.betaFeedbackUnauthenticatedUrl
  override lazy val homeUrl = externalUrls.businessTaxHome
  override lazy val accountDetailsUrl = externalUrls.accountDetails
  override lazy val helpUrl = externalUrls.helpUrl
  override lazy val signOutUrl = externalUrls.survey

  def fallbackURLForLanguageSwitcher: String = loadConfig(s"${env.mode}.languageSwitcher.fallback.url")
  def enableLanguageSwitching: Boolean =
    configuration.getOptional[Boolean](s"${env.mode}.enableLanguageSwitching").getOrElse(false)
  def surveyReOptInPage10Enabled: Boolean =
    configuration.getOptional[Boolean](s"survey.ReOptInPage10.enabled").getOrElse(false)
}
