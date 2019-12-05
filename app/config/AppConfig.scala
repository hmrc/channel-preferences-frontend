package config

import controllers.ExternalUrls
import play.api.Mode.Mode
import play.api.Play.{configuration, current}
import play.api.{Configuration, Play}
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

  override lazy val assetsPrefix                   = ExternalUrls.assets
  override lazy val analyticsToken                 = loadConfig(s"govuk-tax.$env.google-analytics.token")
  override lazy val analyticsHost                  = loadConfig(s"govuk-tax.$env.google-analytics.host")
  override lazy val betaFeedbackUrl                = ExternalUrls.betaFeedbackUrl
  override lazy val betaFeedbackUnauthenticatedUrl = ExternalUrls.betaFeedbackUnauthenticatedUrl
  override lazy val homeUrl                        = ExternalUrls.businessTaxHome
  override lazy val accountDetailsUrl              = ExternalUrls.accountDetails
  override lazy val helpUrl                        = ExternalUrls.helpUrl
  override lazy val signOutUrl                     = ExternalUrls.survey

  def fallbackURLForLanguageSwitcher: String = loadConfig(s"$env.languageSwitcher.fallback.url")
  def enableLanguageSwitching: Boolean = configuration.getBoolean(s"$env.enableLanguageSwitching").getOrElse(false)

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
