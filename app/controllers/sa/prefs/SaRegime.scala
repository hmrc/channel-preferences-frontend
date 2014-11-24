package controllers.sa.prefs

import controllers.common.{GovernmentGateway, UserCredentials}
import play.api.Logger
import play.api.mvc.{AnyContent, Request, Results}
import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import uk.gov.hmrc.common.microservice.domain.TaxRegime

import scala.concurrent.Future

object SaRegime extends TaxRegime {

  def isAuthorised(accounts: Accounts) = accounts.sa.isDefined

  val authenticationType = new GovernmentGateway {
    lazy val login: String = ExternalUrls.signIn
  }
}


object SaRegimeWithoutRedirection extends TaxRegime with Results{

  def isAuthorised(accounts: Accounts) = accounts.sa.isDefined

  val authenticationType = new GovernmentGateway {
    lazy val login: String = ???

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