package controllers

import play.api.mvc._
import controllers.service._
import microservice.domain.{ RegimeRoots, TaxRegime, User }
import microservice.auth.domain.Regimes
import play.Logger
import java.net.URI
import org.slf4j.MDC
import java.util.UUID
import views.html.{ login, server_error }
import play.api.{ Mode, Play }
import com.google.common.net.HttpHeaders
import microservice.HasResponse
import config.{ DateTimeProvider, LastRequestTimestampCookie }
import org.joda.time.{ DateTimeZone, DateTime }

trait HeaderNames {
  val requestId = "X-Request-ID"
  val authorisation = HttpHeaders.AUTHORIZATION
}

trait ActionWrappers extends MicroServices with CookieEncryption with HeaderNames with DateTimeProvider {
  self: Controller =>

  private[ActionWrappers] def act(userId: String, token: Option[String], request: Request[AnyContent], taxRegime: Option[TaxRegime], action: (User) => (Request[AnyContent]) => Result): Result = {

    MDC.put(authorisation, s"$userId")
    MDC.put(requestId, "frontend-" + UUID.randomUUID().toString)

    try {
      val userAuthority = authMicroService.authority(userId)

      Logger.debug(s"Received user authority: $userAuthority")
      userAuthority match {
        case (Some(ua)) => {
          taxRegime match {
            case Some(regime) if !regime.isAuthorised(ua.regimes) =>
              Logger.debug("user not authorised for " + regime.getClass)
              Redirect(regime.unauthorisedLandingPage)
            case _ =>
              val user = User(userId, ua, getRegimeRootsObject(ua.regimes), decrypt(request.session.get("name")), token)
              action(user)(request)
          }
        }
        case _ => {
          Logger.warn(s"No authority found for user id '$userId' from '${request.remoteAddress}'")
          Unauthorized(login()).withNewSession
        }
      }
    } catch {
      case t: Throwable => internalServerError(request, t)
    } finally {
      MDC.clear
    }
  }

  private def sessionIsValid(request: Request[_]): Boolean = {

    val timestampCookie = request.cookies.get(LastRequestTimestampCookie.cookieName).flatMap(LastRequestTimestampCookie(_))

    timestampCookie.isDefined && timestampCookie.get.isValid(now)
  }

  object AuthorisedForIdaAction {

    private def checkAuthorisation(request: Request[_]): Option[String] = {
      if (sessionIsValid(request)) {
        val encryptedUserId: Option[String] = request.session.get("userId")
        val token: Option[String] = request.session.get("token")
        if (encryptedUserId.isDefined && token.isEmpty) {
          Some(encryptedUserId.get)
        } else None
      } else None
    }

    def apply(taxRegime: Option[TaxRegime] = None)(action: (User => (Request[AnyContent] => Result))): Action[AnyContent] = Action {
      request =>
        checkAuthorisation(request) match {
          case None =>
            Logger.debug("No identity cookie found or wrong user type - redirecting to login. user : $userId tokenDefined : ${token.isDefined}")
            Redirect(routes.HomeController.landing()).withNewSession
          case Some(encryptedUserId) =>
            act(decrypt(encryptedUserId), None, request, taxRegime, action)
        }
    }
  }

  object AuthorisedForGovernmentGatewayAction {
    private def checkAuthorisation(request: Request[_]): Option[(String, String)] = {
      if (sessionIsValid(request)) {
        val encryptedUserId: Option[String] = request.session.get("userId")
        val token: Option[String] = request.session.get("token")
        if (encryptedUserId.isDefined && token.isDefined) {
          Some((encryptedUserId.get, token.get))
        } else None
      } else None
    }

    def apply(taxRegime: Option[TaxRegime] = None)(action: (User => (Request[AnyContent] => Result))): Action[AnyContent] = Action {
      request =>
        checkAuthorisation(request) match {
          case None =>
            // the redirect in this condition needs to be reviewed and updated. Important: It will be different from the AuthorisedForIdaAction redirect location
            Logger.debug("No identity cookie found or no gateway token- redirecting to login. user : $userId tokenDefined : ${token.isDefined}")
            Redirect(routes.HomeController.landing()).withNewSession
          case Some((encryptedUserId, token)) =>
            act(decrypt(encryptedUserId), Some(decrypt(token)), request, taxRegime, action)
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
