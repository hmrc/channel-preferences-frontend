package controllers

import play.api.mvc._
import controllers.service._
import microservice.domain.{ RegimeRoots, TaxRegime, User }
import microservice.auth.domain.{ Regimes, UserAuthority }
import play.Logger
import java.net.URI
import org.slf4j.MDC
import java.util.UUID
import views.html.{ login, server_error }
import play.api.{ Mode, Play }

trait HeaderNames {
  val requestId = "X-Request-ID"
  // American spelling because it's an OAuth format header (although we may not strictly be using OAuth)
  val authorisation = "Authorization"
}

trait ActionWrappers extends MicroServices with CookieEncryption with HeaderNames {
  self: Controller =>

  //todo test what happens if user is not authorised to be in this regime - at the time of writing front-end does not do a check
  object AuthorisedForAction {

    def apply[A <: TaxRegime](action: (User => (Request[AnyContent] => Result))): Action[AnyContent] = Action {
      request =>

        request.session.get("userId") match {

          case Some(encryptedUserId) => {

            val userId = decrypt(encryptedUserId)

            MDC.put(authorisation, s"Bearer $userId")
            MDC.put(requestId, "frontend-" + UUID.randomUUID().toString)

            val userAuthority = authMicroService.authority(userId)

            Logger.debug(s"Received user authority: $userAuthority")

            userAuthority match {
              case Some(ua: UserAuthority) => {
                tryAction(request, action(User(user = userId, regimes = getRegimeRootsObject(ua.regimes), userAuthority = ua, nameFromGovernmentGateway = request.session.get("nameFromGovernmentGateway"))))
              }
              case _ => {
                Logger.warn(s"No authority found for user id '$userId' from '${request.remoteAddress}'")
                Unauthorized(login())
              }
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
        tryAction(request, action)
    }
  }

  private def tryAction(request: Request[AnyContent], action: (Request[AnyContent] => Result)): Result = {
    try {
      action(request)
    } catch {
      case t: Throwable => internalServerError(request, t)
    } finally {
      MDC.clear
    }
  }

  private def internalServerError(request: Request[AnyContent], t: Throwable): Result = {
    import play.api.Play.current
    Logger.error("Action failed", t)
    Play.application.mode match {
      // different pages for prod and dev/test
      case Mode.Dev | Mode.Test => InternalServerError(server_error(t, request, MDC.get(requestId)))
      case Mode.Prod => InternalServerError(server_error(t, request, MDC.get(requestId)))
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
    vat = if (regimes.vat.isEmpty) { None } else { Some("#") } //todo change it once we have VAT service / its stub ready
  )

}
