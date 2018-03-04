package config

import javax.inject.Singleton

import play.api.Play._
import uk.gov.hmrc.play.config.ServicesConfig


object StatementsConfig extends ServicesConfig {

  lazy val loginCallbackBaseUrl = getStringOrEmpty("auth.login-callback.base-url")
  lazy val betaFeedbackUrl = s"$getContactServiceUrl/contact/beta-feedback"
  lazy val betaFeedbackUnauthenticatedUrl = s"$getContactServiceUrl/contact/beta-feedback-unauthenticated"
  lazy val reportAProblemPartialUrl = s"$getContactServiceUrl/contact/problem_reports"
  lazy val reportAProblemNonJSUrl = s"$getContactServiceUrl/contact/problem_reports_nonjs?service=AOSS"
  lazy val portalUrl = getRequiredString(s"govuk-tax.$env.portal.url")
  lazy val agentHomeUrl = s"$getAgentServiceUrl/agent/dashboard"
  lazy val agentClientListUrl = s"$getAgentServiceUrl/agent/epaye/client-list"
  lazy val btaManageAccount = s"$getBtaFrontendUrl/business-account/manage-account"
  lazy val btaHome = s"$getBtaFrontendUrl/business-account"
  lazy val btaHelp = s"$getBtaFrontendUrl/business-account/help"
  lazy val btaSurvey = s"$getBtaFrontendUrl/business-account/sso-sign-out"
  lazy val webchatId = configuration.getString(s"$env.webchat.id")

  lazy val assetsPrefix = {
    val assetsUrl = getRequiredString(s"assets.url")
    val assetsVersion = getRequiredString(s"frontend.assets.version")
    s"$assetsUrl$assetsVersion"
  }

  lazy val companyAuthSignInUrl = {
    val companyAuthHost = getStringOrEmpty(s"$env.microservice.services.company-auth.host")
    val companyAuthSignInPath = getStringOrEmpty(s"$env.microservice.services.company-auth.sign-in-path")
    s"$companyAuthHost$companyAuthSignInPath"
  }

  lazy val taxServiceGovUkUrl = getStringOrEmpty("tax-service.gov.uk.url")

  def enableLanguageSwitching = configuration.getBoolean(s"govuk-tax.$env.enableLanguageSwitching").getOrElse(false)

  private def getRequiredString(key: String) =
    configuration.getString(key).getOrElse(throw configuration.reportError(key, s"Mandatory configuration missing, key=$key"))

  private def getStringOrEmpty(key: String): String = configuration.getString(key).getOrElse("")

  private def getContactServiceUrl = getStringOrEmpty(s"$env.microservice.services.contact-frontend.url")

  private def getAgentServiceUrl = getStringOrEmpty(s"$env.microservice.services.agent-service.url")

  private def getBtaFrontendUrl = getStringOrEmpty(s"$env.microservice.services.yta-frontend.url")
}