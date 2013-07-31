package controllers

import play.api.mvc._
import controllers.service._
import microservice.domain.{ RegimeRoots, TaxRegime, User }
import microservice.auth.domain.{ Regimes, UserAuthority }
import play.{ mvc, Logger }
import java.net.URI
import org.slf4j.MDC
import java.util.UUID
import views.html.{ login, server_error }
import play.api.{ Mode, Play }
import com.google.common.net.HttpHeaders
import microservice.{ HasResponse, UnauthorizedException, MicroServiceException }

trait HeaderNames {
  val requestId = "X-Request-ID"
  val authorisation = HttpHeaders.AUTHORIZATION
}

trait ActionWrappers extends MicroServices with CookieEncryption with HeaderNames {
  self: Controller =>

  //todo test what happens if user is not authorised to be in this regime - at the time of writing front-end does not do a check
  object AuthorisedForAction {

    def apply(taxRegime: Option[TaxRegime] = None)(action: (User => (Request[AnyContent] => Result))): Action[AnyContent] = Action {
      request =>
        request.session.get("userId") match {

          case Some(encryptedUserId) => {

            val userId = decrypt(encryptedUserId)

            MDC.put(authorisation, s"$userId")
            MDC.put(requestId, "frontend-" + UUID.randomUUID().toString)

            try {
              val userAuthority = authMicroService.authority(userId)

              Logger.debug(s"Received user authority: $userAuthority")
              val governmentGatewayToken = request.session.get("token")
              (userAuthority, governmentGatewayToken) match {
                case (Some(ua), None) => {
                  taxRegime match {
                    case Some(regime) if !regime.isAuthorised(ua.regimes) =>
                      Logger.debug("user not authorised for " + regime.getClass)
                      Redirect(regime.unauthorisedLandingPage)
                    case _ =>
                      val user = User(userId, ua, getRegimeRootsObject(ua.regimes), decrypt(request.session.get("name")), None)
                      action(user)(request)
                  }
                }
                case _ => {
                  Logger.warn(s"No authority or a government gateway token was found for user id '$userId' from '${request.remoteAddress}'")
                  Unauthorized(login())
                }
              }
            } catch {
              case t: Throwable => internalServerError(request, t)
            } finally {
              MDC.clear
            }
          }
          case None => {
            Logger.debug("No identity cookie found - redirecting to login.")
            Redirect(routes.HomeController.landing())
          }
        }
    }
  }

  object AuthorisedForGovernmentGatewayAction {

    def apply(taxRegime: Option[TaxRegime] = None)(action: (User => (Request[AnyContent] => Result))): Action[AnyContent] = Action {
      request =>
        request.session.get("userId") match {

          case Some(encryptedUserId) => {

            val userId = decrypt(encryptedUserId)

            MDC.put(authorisation, s"$userId")
            MDC.put(requestId, "frontend-" + UUID.randomUUID().toString)

            try {
              val userAuthority = authMicroService.authority(userId)

              Logger.debug(s"Received user authority: $userAuthority")
              val governmentGatewayToken = request.session.get("token")
              Logger.debug("received token : " + governmentGatewayToken.isDefined)
              (userAuthority, governmentGatewayToken) match {
                case (Some(ua), Some(token)) => {
                  taxRegime match {
                    case Some(regime) if !regime.isAuthorised(ua.regimes) =>
                      Logger.debug("user not authorised for " + regime.getClass)
                      Redirect(regime.unauthorisedLandingPage)
                    case _ =>
                      val user = User(userId, ua, getRegimeRootsObject(ua.regimes), decrypt(request.session.get("name")), decrypt(Some(token)))
                      action(user)(request)
                  }
                }
                case _ => {
                  Logger.warn(s"No authority or no token found for user id '$userId' from '${request.remoteAddress}'")
                  Unauthorized(login())
                }
              }
            } catch {
              case t: Throwable => internalServerError(request, t)
            } finally {
              MDC.clear
            }
          }
          case None => {
            Logger.debug("No identity cookie found - redirecting to login.")
            Redirect(routes.HomeController.landing())
          }
        }
    }
  }

  object UnauthorisedAction {
    def apply[A <: TaxRegime](action: (Request[AnyContent] => Result)): Action[AnyContent] = Action {
      request =>

        MDC.put(requestId, "frontend-" + UUID.randomUUID().toString)

        try {
          action(request)
        } catch {
          case t: Throwable => internalServerError(request, t)
        } finally {
          MDC.clear
        }
    }
  }

  private def internalServerError(request: Request[AnyContent], t: Throwable): Result = {
    logThrowable(t)
    import play.api.Play.current
    Play.application.mode match {
      // different pages for prod and dev/test
      case Mode.Dev | Mode.Test => InternalServerError(server_error(t, request, MDC.get(requestId)))
      case Mode.Prod => InternalServerError(server_error(t, request, MDC.get(requestId)))
    }
  }

  private def logThrowable(t: Throwable) {
    Logger.error("Action failed", t)
    if (t.isInstanceOf[HasResponse]) {
      Logger.error(s"MicroService Response '${t.asInstanceOf[HasResponse].response.body}'")
    }
  }

  private def getRegimeRootsObject(regimes: Regimes): RegimeRoots = RegimeRoots(
    paye = regimes.paye match {
      case Some(x: URI) => Some(payeMicroService.root(x.toString))
      case _ => None
    },
    sa = regimes.sa match {
      case Some(x: URI) => Some(saMicroService.root(x.toString))
      case _ => None
    },
    vat = if (regimes.vat.isEmpty) {
      None
    } else {
      Some("#")
    } //todo change it once we have VAT service / its stub ready
  )

}
