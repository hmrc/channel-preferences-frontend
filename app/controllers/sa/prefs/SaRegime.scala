package controllers.sa.prefs

import play.api.Logger
import play.api.mvc.{AnyContent, Request, Result, Results}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.frontend.auth.{UserCredentials, GovernmentGateway, TaxRegime}

import scala.concurrent.Future

object SaRegime extends TaxRegime {

  val authenticationType = new GovernmentGateway {
    lazy val login: String = ExternalUrls.signIn
  }

  override def isAuthorised(accounts: Accounts): Boolean = accounts.sa.isDefined
}


object SaRegimeWithoutRedirection extends TaxRegime with Results {

  def isAuthorised(accounts: Accounts) = accounts.sa.isDefined

  val authenticationType = new GovernmentGateway {
    lazy val login: String = throw new IllegalStateException("Should be no redirect to login")

    override def redirectToLogin(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): Future[Result] = Future.successful(Unauthorized)

    override def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) = {
      case UserCredentials(None, token@_) =>
        Logger.info(s"No userId found - unauthorized. user: None token : $token")
        Future.successful(Right(Unauthorized))
      case UserCredentials(Some(userId), None) =>
        Logger.info(s"No gateway token - unauthorized. user : $userId token : None")
        Future.successful(Right(Unauthorized))
    }
  }
}