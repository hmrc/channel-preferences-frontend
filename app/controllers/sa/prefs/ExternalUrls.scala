package controllers.sa.prefs

import play.api.Play
import uk.gov.hmrc.play.config.RunMode

object ExternalUrlPrefixes extends RunMode {
  import play.api.Play.current
  val pfUrlPrefix             = Play.configuration.getString(s"govuk-tax.$env.preferences-frontend.host").getOrElse("")
  val ytaUrlPrefix            = Play.configuration.getString(s"govuk-tax.$env.yta.host").getOrElse("")
  val taiUrlPrefix            = Play.configuration.getString(s"govuk-tax.$env.tai.host").getOrElse("")
  val caUrlPrefix             = Play.configuration.getString(s"govuk-tax.$env.company-auth.host").getOrElse("")
}

object ExternalUrls extends RunMode {
  import play.api.Play.current
  import ExternalUrlPrefixes._

  val betaFeedbackUrl                = s"$caUrlPrefix/contact/beta-feedback"
  val betaFeedbackUnauthenticatedUrl = s"$caUrlPrefix/contact/beta-feedback-unauthenticated"
  val helpUrl                        = s"$caUrlPrefix/contact/contact-hmrc"

  val changeEmailAddress             = s"$pfUrlPrefix/account/account-details/sa/update-email-address"
  val optOutOfEmailReminders         = s"$pfUrlPrefix/account/account-details/sa/opt-out-email-reminders"
  val displayPreferences             = s"$pfUrlPrefix/account/account-details/sa/opt-in-email-reminders"

  val accountDetails                 = s"$ytaUrlPrefix/account/account-details"
  val businessTaxHome                = s"$ytaUrlPrefix/account"
  val survey                         = s"$businessTaxHome/survey"
  val yourIncomeTax                  = s"$taiUrlPrefix/check-income-tax/your-income-tax"
  val taiSignOutUrl                  = s"$taiUrlPrefix/check-income-tax/signout"

  val loginCallback                  = Play.configuration.getString(s"govuk-tax.$env.login-callback.url").getOrElse(businessTaxHome)
  val signIn                         = s"$caUrlPrefix/account/sign-in?continue=$loginCallback"

  val assets                         = Play.configuration.getString(s"govuk-tax.$env.assets.url").getOrElse(throw new RuntimeException("no assets url set")) +
                                       Play.configuration.getString(s"govuk-tax.$env.assets.version").getOrElse(throw new RuntimeException("no assets version set"))
}
