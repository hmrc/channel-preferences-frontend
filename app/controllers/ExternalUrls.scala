/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import play.api.{ Configuration, Environment }

import javax.inject.{ Inject, Singleton }

@Singleton
class ExternalUrlPrefixes @Inject() (configuration: Configuration, env: Environment) {
  lazy val pfUrlPrefix =
    configuration.getOptional[String](s"preferences-frontend.host").getOrElse("")
  lazy val ytaUrlPrefix = configuration.getOptional[String](s"yta.host").getOrElse("")
  lazy val taiUrlPrefix = configuration.getOptional[String](s"tai.host").getOrElse("")
  lazy val caUrlPrefix = configuration.getOptional[String](s"company-auth.host").getOrElse("")

}

@Singleton
class ExternalUrls @Inject() (
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
    configuration.getOptional[String](s"login-callback.url").getOrElse(businessTaxHome)
  lazy val assets = configuration
    .getOptional[String](s"assets.url")
    .getOrElse(throw new RuntimeException("no assets url set")) +
    configuration
      .getOptional[String](s"assets.version")
      .getOrElse(throw new RuntimeException("no assets version set"))
}
