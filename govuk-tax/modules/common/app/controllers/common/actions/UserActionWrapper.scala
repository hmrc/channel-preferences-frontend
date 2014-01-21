package controllers.common.actions

import play.api.mvc._
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, TaxRegime, User}
import play.api.Logger
import controllers.common.{AnyAuthenticationProvider, SessionKeys, UserCredentials, AuthenticationProvider}
import uk.gov.hmrc.common.microservice.auth.AuthConnector

import scala.Some
import play.api.mvc.SimpleResult
import scala.concurrent._
import uk.gov.hmrc.common.MdcLoggingExecutionContext.fromLoggingDetails
import uk.gov.hmrc.common.microservice.auth.domain.Authority


trait UserActionWrapper
  extends Results {

  protected implicit val authConnector: AuthConnector

  private[actions] def WithUserAuthorisedBy(authenticationProvider: AuthenticationProvider,
                                            taxRegime: Option[TaxRegime],
                                            redirectToOrigin: Boolean)
                                           (userAction: User => Action[AnyContent]): Action[AnyContent] =
    Action.async { request =>
      implicit val hc = HeaderCarrier(request)
      Logger.debug(s"WithUserAuthorisedBy using auth provider ${authenticationProvider.id}")
      val handle = authenticationProvider.handleNotAuthenticated(request, redirectToOrigin) orElse handleAuthenticated(request, taxRegime, authenticationProvider)

      handle(UserCredentials(request.session)).flatMap {
        case Left(successfullyFoundUser) => userAction(successfullyFoundUser)(request)
        case Right(resultOfFailure) => Action(resultOfFailure)(request)
      }
    }

  private def handleAuthenticated(request: Request[AnyContent], taxRegime: Option[TaxRegime], authenticationProvider: AuthenticationProvider): PartialFunction[UserCredentials, Future[Either[User, SimpleResult]]] = {
    case UserCredentials(Some(userId), tokenOption) =>
      implicit val hc = HeaderCarrier(request)
      val authority = authConnector.authority(userId)
      Logger.debug(s"Received user authority: $authority")

      authority.flatMap {
        case Some(ua) => taxRegime match {
          case Some(regime) if !regime.isAuthorised(ua.accounts) =>
            Logger.info("user not authorised for " + regime.getClass)
            Future.successful(Right(Redirect(regime.unauthorisedLandingPage)))
          case _ =>
            regimeRoots(ua).map { regimeRoots =>
              Left(User(
                userId = userId,
                userAuthority = ua,
                regimes = regimeRoots,
                nameFromGovernmentGateway = request.session.get(SessionKeys.name),
                decryptedToken = tokenOption))
            }
        }
        case _ =>
          Logger.warn(s"No authority found for user id '$userId' from '${request.remoteAddress}'")
          Future.successful(Right(AnyAuthenticationProvider.redirectToLogin(request).withNewSession))
      }
  }

  def regimeRoots(authority: Authority)(implicit hc: HeaderCarrier): Future[RegimeRoots]
}