package controllers.sa.prefs

import uk.gov.hmrc.play.frontend.auth.AuthContext

object AuthContextAvailability {
  implicit def oac(implicit ac: AuthContext): Option[AuthContext] = Option(ac)
}
