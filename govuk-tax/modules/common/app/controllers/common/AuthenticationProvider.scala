package controllers.common

import scala.concurrent._
import scala._

import play.api.mvc._

import controllers.common.FrontEndRedirect._
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.MdcLoggingExecutionContext
import MdcLoggingExecutionContext.fromLoggingDetails
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.SimpleResult
import play.api.Logger
import uk.gov.hmrc.common.microservice.idatokenapi.IdaTokenApiConnector


sealed trait AuthenticationProvider {
  type FailureResult = SimpleResult
  val id: String

  def redirectToLogin(redirectToOrigin: Boolean = false)(implicit request: Request[AnyContent]): Future[SimpleResult]

  def handleSessionTimeout()(implicit request: Request[AnyContent]): Future[SimpleResult] = redirectToLogin(false)

  def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): PartialFunction[UserCredentials, Future[Either[User, FailureResult]]]

  implicit def hc(implicit request: Request[_]) = HeaderCarrier(request)
}

case class UserCredentials(userId: Option[String], token: Option[String])

object UserCredentials {

  import SessionKeys._

  def apply(session: Session): UserCredentials = UserCredentials(session.get(userId), session.get(token))
}

object Ida extends AuthenticationProvider {
  val id = "IDA"

  val login = routes.IdaLoginController.samlLogin

  override def handleSessionTimeout()(implicit request: Request[AnyContent]): Future[SimpleResult] =
    Future.successful(Redirect(routes.LoginController.payeSignedOut()))

  def redirectToLogin(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): Future[SimpleResult] =
    Future.successful(Redirect(login).withSession(buildSessionForRedirect(request.session, redirectUrl(redirectToOrigin))))

  private def redirectUrl(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) =
    if (redirectToOrigin) Some(request.uri) else None

  def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) = {
    case UserCredentials(None, token@_) =>
      Logger.info(s"No identity cookie found - redirecting to login. user: None token : $token")
      redirectToLogin(redirectToOrigin).map(Right(_))
    case UserCredentials(Some(userId), Some(token)) =>
      Logger.info(s"Wrong user type - redirecting to login. user : $userId token : $token")
      redirectToLogin(redirectToOrigin).map(Right(_))
  }
}

/**
 * BETA: We're using this provider in beta to handle the IDA token. After beta we will
 * revert to the implementation above.
 */
object IdaWithTokenCheckForBeta extends AuthenticationProvider {

  val id = "IDA"

  lazy val idaTokenApiConnector = new IdaTokenApiConnector

  override def handleSessionTimeout()(implicit request: Request[AnyContent]): Future[SimpleResult] =
    Future.successful(Redirect(routes.LoginController.payeSignedOut()))

  def redirectToLogin(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): Future[SimpleResult] = {
    implicit val hc = HeaderCarrier(request)
    getTokenFromRequest match {
      case Some(token) => {
        idaTokenApiConnector.validateToken(token).map { isValid =>
          if (isValid) toSamlLogin.withSession(buildSessionForRedirect(request.session, redirectUrl(request, true)))
          else {
            Logger.info("The provided Ida token is not valid")
            toBadIdaToken
          }
        }
      }
      case None => {
        if (idaTokenApiConnector.idaTokenRequired) Future.successful(toBadIdaToken)
        else Future.successful(toSamlLogin.withSession(buildSessionForRedirect(request.session, redirectUrl(request, true))))
      }
    }
  }

  private[controllers] def getTokenFromRequest(implicit request: Request[AnyContent]): Option[String] = {
    request.getQueryString("token").flatMap {
      token =>
        if (token.isEmpty){
          Logger.info("The provided Ida token is empty")
          None
        } else Some(token)
    }
  }

  private def toBadIdaToken = {
    Logger.info("Redirecting to bad Ida token page")
    Redirect("/paye/company-car/ida-token-required-in-beta")
  }

  private def redirectUrl(request: Request[AnyContent], redirectToOrigin: Boolean) =
    if (redirectToOrigin) Some(request.uri) else None

  def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): PartialFunction[UserCredentials, Future[Either[User, SimpleResult]]] = {
    case UserCredentials(None, token@_) =>
      implicit val hc = HeaderCarrier(request)
      Logger.info(s"No identity cookie found - redirecting to login. user: None token : $token")
      redirectToLogin(redirectToOrigin).map(result => Right(result))
    case UserCredentials(Some(userId), Some(token)) =>
      implicit val hc = HeaderCarrier(request)
      Logger.info(s"Wrong user type - redirecting to login. user : $userId token : $token")
      redirectToLogin(redirectToOrigin).map(result => Right(result))
  }
}

object GovernmentGateway extends AuthenticationProvider {
  val id = "GGW"

  val login = routes.LoginController.businessTaxLogin()

  def redirectToLogin(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) = Future.successful(Redirect(login))

  def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): PartialFunction[UserCredentials, Future[Either[User, FailureResult]]] = {
    case UserCredentials(None, token@_) =>
      Logger.info(s"No identity cookie found - redirecting to login. user: None token : $token")
      redirectToLogin(redirectToOrigin).map(Right(_))
    case UserCredentials(Some(userId), None) =>
      Logger.info(s"No gateway token - redirecting to login. user : $userId token : None")
      redirectToLogin(redirectToOrigin).map(Right(_))
  }
}

object AnyAuthenticationProvider extends AuthenticationProvider {

  private val login = routes.LoginController.businessTaxLogin()

  def redirectToLogin(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]): Future[SimpleResult] = {
    Logger.info("In AnyAuthenticationProvider - redirecting to login page")
    request.session.get(SessionKeys.authProvider) match {
      case Some(IdaWithTokenCheckForBeta.id) => IdaWithTokenCheckForBeta.redirectToLogin(redirectToOrigin)
      case _ => Future.successful(Redirect(login))
    }
  }

  def handleNotAuthenticated(redirectToOrigin: Boolean)(implicit request: Request[AnyContent]) = {
    request.session.get(SessionKeys.authProvider) match {
      case Some(GovernmentGateway.id) => GovernmentGateway.handleNotAuthenticated(redirectToOrigin)
      case Some(IdaWithTokenCheckForBeta.id) => IdaWithTokenCheckForBeta.handleNotAuthenticated(redirectToOrigin)
      case _ => {
        case _ => Future.successful(Right(Redirect(login).withNewSession))
      }
    }
  }

  val id = "IDAorGGW"
}


