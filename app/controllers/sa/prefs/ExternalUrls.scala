package controllers.sa.prefs

import uk.gov.hmrc.play.config.RunMode
import play.api.Play

object ExternalUrls extends RunMode {
  import play.api.Play.current
  val ytaHost          = s"${Play.configuration.getString(s"govuk-tax.$env.yta.host").getOrElse("")}"
  val accountDetails    = s"$ytaHost/account/account-details"
  val businessTaxHome = s"$ytaHost/account"
  val signOut          = s"$ytaHost/sign-out"
}
