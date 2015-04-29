package controllers.sa.prefs.config

import controllers.sa.prefs.ExternalUrls
import play.api.Play.{configuration, current}
import uk.gov.hmrc.play.config.RunMode

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

object YtaConfig extends AppConfig with RunMode {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new RuntimeException(s"Missing key: $key"))

  override lazy val assetsPrefix = ExternalUrls.assets
  override lazy val analyticsToken = loadConfig(s"govuk-tax.$env.google-analytics.token")
  override lazy val analyticsHost = loadConfig(s"govuk-tax.$env.google-analytics.host")
  override lazy val betaFeedbackUrl = s"${ExternalUrls.caUrlPrefix}/contact/beta-feedback"
  override lazy val betaFeedbackUnauthenticatedUrl = s"${ExternalUrls.caUrlPrefix}/contact/beta-feedback-unauthenticated"
  override lazy val homeUrl = ExternalUrls.businessTaxHome
  override lazy val accountDetailsUrl = ExternalUrls.accountDetails
  override lazy val helpUrl = s"${ExternalUrls.caUrlPrefix}/contact/contact-hmrc"
  override lazy val signOutUrl = ExternalUrls.survey
}
