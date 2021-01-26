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

package controllers

import play.api.{ Configuration, Environment }

import javax.inject.{ Inject, Singleton }

@Singleton
class ExternalUrlPrefixes @Inject()(configuration: Configuration, env: Environment) {
  lazy val pfUrlPrefix =
    configuration.getOptional[String](s"govuk-tax.${env.mode}.preferences-frontend.host").getOrElse("")
  lazy val ytaUrlPrefix = configuration.getOptional[String](s"govuk-tax.${env.mode}.yta.host").getOrElse("")
  lazy val taiUrlPrefix = configuration.getOptional[String](s"govuk-tax.${env.mode}.tai.host").getOrElse("")
  lazy val caUrlPrefix = configuration.getOptional[String](s"govuk-tax.${env.mode}.company-auth.host").getOrElse("")

}

@Singleton
class ExternalUrls @Inject()(
  configuration: Configuration,
  env: Environment,
  externalUrlPrefixes: ExternalUrlPrefixes
) {

  lazy val betaFeedbackUrl = s"${externalUrlPrefixes.caUrlPrefix}/contact/beta-feedback"
  lazy val betaFeedbackUnauthenticatedUrl = s"${externalUrlPrefixes.caUrlPrefix}/contact/beta-feedback-unauthenticated"
  lazy val helpUrl = s"${externalUrlPrefixes.caUrlPrefix}/contact/contact-hmrc"

  lazy val manageAccount = s"${externalUrlPrefixes.ytaUrlPrefix}/account/account-details"
  lazy val accountDetails = manageAccount
  lazy val businessTaxHome = s"${externalUrlPrefixes.ytaUrlPrefix}/account"
  lazy val survey = s"$businessTaxHome/sso-sign-out"

  lazy val loginCallback =
    configuration.getOptional[String](s"govuk-tax.${env.mode}.login-callback.url").getOrElse(businessTaxHome)
  lazy val assets = configuration
    .getOptional[String](s"govuk-tax.${env.mode}.assets.url")
    .getOrElse(throw new RuntimeException("no assets url set")) +
    configuration
      .getOptional[String](s"govuk-tax.${env.mode}.assets.version")
      .getOrElse(throw new RuntimeException("no assets version set"))
}
