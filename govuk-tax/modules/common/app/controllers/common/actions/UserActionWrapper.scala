package controllers.common.actions

import play.api.mvc._
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, TaxRegime, User}
import play.api.Logger
import controllers.common.{CookieEncryption, AuthenticationProvider}
import uk.gov.hmrc.common.microservice.auth.AuthConnector

import scala.Some
import play.api.mvc.SimpleResult
import views.html.login
import scala.concurrent._
import ExecutionContext.Implicits.global
import uk.gov.hmrc.common.microservice.auth.domain.{Authority, UserAuthority}


trait UserActionWrapper
  extends Results
  with CookieEncryption {

  protected implicit val authConnector: AuthConnector

  private[actions] def WithUserAuthorisedBy(authenticationProvider: AuthenticationProvider,
                                            taxRegime: Option[TaxRegime],
                                            redirectToOrigin: Boolean)
                                           (userAction: User => Action[AnyContent]): Action[AnyContent] =
    Action.async { request =>
      val handle = authenticationProvider.handleNotAuthenticated(request, redirectToOrigin) orElse handleAuthenticated(request, taxRegime)

      handle((request.session.get("userId"), request.session.get("token"))).flatMap {
        case Left(successfullyFoundUser) => userAction(successfullyFoundUser)(request)
        case Right(resultOfFailure) => Action(resultOfFailure)(request)
      }
    }

  private def handleAuthenticated(request: Request[AnyContent], taxRegime: Option[TaxRegime]): PartialFunction[(Option[String], Option[String]), Future[Either[User, SimpleResult]]] = {
    case (Some(encryptedUserId), tokenOption) =>

      implicit val hc = HeaderCarrier(request)
      val userId = decrypt(encryptedUserId)
      val token = tokenOption.map(decrypt)
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
                nameFromGovernmentGateway = decrypt(request.session.get("name")),
                decryptedToken = token))
            }
        }
        case _ => {
          Logger.warn(s"No authority found for user id '$userId' from '${request.remoteAddress}'")
          Future.successful(Right(Unauthorized(login()).withNewSession))
        }
      }
  }

  def regimeRoots(authority: Authority)(implicit hc: HeaderCarrier): Future[RegimeRoots]
}