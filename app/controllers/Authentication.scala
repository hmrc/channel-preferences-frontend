package controllers

import authentication.ValidSessionCredentialsProvider
import uk.gov.hmrc.play.frontend.auth.Actions

trait Authentication extends Actions {
  def authenticated =
    AuthenticatedBy(ValidSessionCredentialsProvider, pageVisibility = GGConfidence)
}
