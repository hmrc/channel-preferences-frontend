package controllers.common.actions

import scala.concurrent._
import play.api.mvc._
import play.api.Logger
import play.api.mvc.SimpleResult

import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.domain.TaxRegime
import uk.gov.hmrc.common.MdcLoggingExecutionContext.fromLoggingDetails
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.auth.domain.Authority

import controllers.common._

trait UserActionWrapper
  extends Results {

  protected implicit val authConnector: AuthConnector

  private[actions] def WithUserAuthorisedBy(authenticationProvider: AuthenticationProvider,
                                            taxRegime: Option[TaxRegime],
                                            redirectToOrigin: Boolean)
                                           (userAction: User => Action[AnyContent]): Action[AnyContent] =
    Action.async { implicit request =>
      implicit val hc = HeaderCarrier(request)
      Logger.info(s"WithUserAuthorisedBy using auth provider ${authenticationProvider.id}")
      val handle = authenticationProvider.handleNotAuthenticated(redirectToOrigin) orElse handleAuthenticated(taxRegime, authenticationProvider)

      handle(UserCredentials(request.session)).flatMap {
        case Left(successfullyFoundUser) => userAction(successfullyFoundUser)(request)
        case Right(resultOfFailure) => Action(resultOfFailure)(request)
      }
    }

  private def handleAuthenticated(taxRegime: Option[TaxRegime], authenticationProvider: AuthenticationProvider)
                                 (implicit request: Request[AnyContent]):
  PartialFunction[UserCredentials, Future[Either[User, SimpleResult]]] = {
    case UserCredentials(Some(userId), tokenOption) =>
      implicit val hc = HeaderCarrier(request)
      val authority = authConnector.currentAuthority
      Logger.debug(s"Received user authority: $authority")

      authority.flatMap {
        case Some(ua) => taxRegime match {
          case Some(regime) if !regime.isAuthorised(ua.accounts) =>
            Logger.info("user not authorised for " + regime.getClass)
            authenticationProvider.redirectToLogin(false).map(Right(_))
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
        case _ => {
          Logger.warn(s"No authority found for user id '$userId' from '${request.remoteAddress}'")
          authenticationProvider.redirectToLogin(false).map { result =>
            Right(result.withNewSession)
          }
        }
      }
  }

  def regimeRoots(authority: Authority)(implicit hc: HeaderCarrier): Future[RegimeRoots]
}