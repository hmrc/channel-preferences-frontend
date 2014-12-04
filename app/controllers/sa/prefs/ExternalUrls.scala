package controllers.sa.prefs

import uk.gov.hmrc.play.config.RunMode
import play.api.Play

object ExternalUrls extends RunMode {
  import play.api.Play.current
  val pfHost                  = Play.configuration.getString(s"govuk-tax.$env.preferences-frontend.host").getOrElse("")
  val ytaHost                 = Play.configuration.getString(s"govuk-tax.$env.yta.host").getOrElse("")
  val caHost                  = Play.configuration.getString(s"govuk-tax.$env.company-auth.host").getOrElse("")

  val resendValidationUrl     = s"$pfHost/account/account-details/sa/resend-validation-email"
  val changeEmailAddress      = s"$pfHost/account/account-details/sa/update-email-address"
  val optOutOfEmailReminders  = s"$pfHost/account/account-details/sa/opt-out-email-reminders"

  val accountDetails          = s"$ytaHost/account/account-details"
  val businessTaxHome         = s"$ytaHost/account"
  val survey                  = s"$businessTaxHome/survey"

  val loginCallback           = Play.configuration.getString(s"govuk-tax.$env.login-callback.url").getOrElse(businessTaxHome)
  val signIn                  = s"$caHost/account/sign-in?continue=$loginCallback"
}
