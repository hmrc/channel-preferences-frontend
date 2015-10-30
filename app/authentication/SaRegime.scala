package authentication

import controllers.sa.prefs.ExternalUrls
import play.api.Logger
import play.api.mvc.{AnyContent, Request, Results}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.frontend.auth.{AuthenticationProvider, GovernmentGateway, TaxRegime, UserCredentials}

import scala.concurrent.Future

object SaRegime extends TaxRegime {

  val authenticationType = new GovernmentGateway {
    lazy val login: String = ExternalUrls.signIn
  }

  override def isAuthorised(accounts: Accounts): Boolean = accounts.sa.isDefined
}

object SaRegimeWithoutRedirection extends TaxRegime with Results {

  def isAuthorised(accounts: Accounts) = accounts.sa.isDefined

  val authenticationType = new AuthenticationProvider with Results {
    def id: String = "ValidSessionCredentials"

    lazy val login: String = throw new IllegalStateException("Should be no redirect to login")

    def redirectToLogin(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) =
      Future.successful(Unauthorized)

    def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) = {
      case UserCredentials(None, token) =>
        Logger.info(s"No userId found - unauthorized. user: None token : $token")
        Future.successful(Right(Unauthorized))
    }
  }
}
