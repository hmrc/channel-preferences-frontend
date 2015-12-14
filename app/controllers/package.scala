import authentication.ValidSessionCredentialsProvider
import uk.gov.hmrc.play.frontend.auth.Actions

package object controllers {

  trait Authentication extends Actions {
    def authenticated =
      AuthenticatedBy(ValidSessionCredentialsProvider, pageVisibility = GGConfidence)
  }

}
