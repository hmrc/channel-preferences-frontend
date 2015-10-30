package controllers.sa.prefs

import play.api.Play
import uk.gov.hmrc.play.config.RunMode

object ExternalUrlPrefixes extends RunMode {
  import play.api.Play.current
  lazy val pfUrlPrefix             = Play.configuration.getString(s"govuk-tax.$env.preferences-frontend.host").getOrElse("")
  lazy val ytaUrlPrefix            = Play.configuration.getString(s"govuk-tax.$env.yta.host").getOrElse("")
  lazy val taiUrlPrefix            = Play.configuration.getString(s"govuk-tax.$env.tai.host").getOrElse("")
  lazy val caUrlPrefix             = Play.configuration.getString(s"govuk-tax.$env.company-auth.host").getOrElse("")
}

object ExternalUrls extends RunMode {
  import play.api.Play.current
  import ExternalUrlPrefixes._

  lazy val betaFeedbackUrl                = s"$caUrlPrefix/contact/beta-feedback"
  lazy val betaFeedbackUnauthenticatedUrl = s"$caUrlPrefix/contact/beta-feedback-unauthenticated"
  lazy val helpUrl                        = s"$caUrlPrefix/contact/contact-hmrc"

  lazy val manageAccount                  = s"$ytaUrlPrefix/account/account-details"
  lazy val accountDetails                 = manageAccount
  lazy val businessTaxHome                = s"$ytaUrlPrefix/account"
  lazy val survey                         = s"$businessTaxHome/survey"

  lazy val loginCallback                  = Play.configuration.getString(s"govuk-tax.$env.login-callback.url").getOrElse(businessTaxHome)
  lazy val signIn                         = s"$caUrlPrefix/account/sign-in?continue=$loginCallback"

  lazy val assets                         = Play.configuration.getString(s"govuk-tax.$env.assets.url").getOrElse(throw new RuntimeException("no assets url set")) +
                                            Play.configuration.getString(s"govuk-tax.$env.assets.version").getOrElse(throw new RuntimeException("no assets version set"))
}
