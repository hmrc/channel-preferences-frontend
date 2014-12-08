package controllers.sa.prefs

import uk.gov.hmrc.play.config.RunMode
import play.api.Play

object ExternalUrls extends RunMode {
  import play.api.Play.current
  val pfUrlPrefix             = Play.configuration.getString(s"govuk-tax.$env.preferences-frontend.urlPrefix").getOrElse("")
  val ytaUrlPrefix            = Play.configuration.getString(s"govuk-tax.$env.yta.urlPrefix").getOrElse("")
  val caUrlPrefix             = Play.configuration.getString(s"govuk-tax.$env.company-auth.urlPrefix").getOrElse("")

  val resendValidationUrl     = s"$pfUrlPrefix/account/account-details/sa/resend-validation-email"
  val changeEmailAddress      = s"$pfUrlPrefix/account/account-details/sa/update-email-address"
  val optOutOfEmailReminders  = s"$pfUrlPrefix/account/account-details/sa/opt-out-email-reminders"
  val displayPreferences      = s"$pfUrlPrefix/account/account-details/sa/opt-in-email-reminders"

  val accountDetails          = s"$ytaUrlPrefix/account/account-details"
  val businessTaxHome         = s"$ytaUrlPrefix/account"
  val survey                  = s"$businessTaxHome/survey"

  val loginCallback           = Play.configuration.getString(s"govuk-tax.$env.login-callback.url").getOrElse(businessTaxHome)
  val signIn                  = s"$caUrlPrefix/account/sign-in?continue=$loginCallback"
}
