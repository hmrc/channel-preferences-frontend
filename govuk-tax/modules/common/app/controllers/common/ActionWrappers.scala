package controllers.common

import play.api.mvc._
import controllers.common.service._
import uk.gov.hmrc.common.microservice.domain.{ RegimeRoots, TaxRegime, User }
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import java.net.URI
import views.html.login
import com.google.common.net.HttpHeaders
import play.api.mvc.Result
import play.api.Logger
import controllers.common.actions.{ LoggingActionWrapper, AuditActionWrapper, HeaderActionWrapper }

trait HeaderNames {
  val requestId = "X-Request-ID"
  val authorisation = HttpHeaders.AUTHORIZATION
  val forwardedFor = "x-forwarded-for"
}

object HeaderNames extends HeaderNames

trait ActionWrappers extends MicroServices with Results with CookieEncryption with HeaderActionWrapper with AuditActionWrapper with LoggingActionWrapper {

  private[ActionWrappers] def act(userId: String, token: Option[String], request: Request[AnyContent], taxRegime: Option[TaxRegime], action: (User) => (Request[AnyContent]) => Result): Result = {

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
  }

  object AuthorisedForIdaAction {

    def apply(taxRegime: Option[TaxRegime] = None)(action: (User => (Request[AnyContent] => Result))): Action[AnyContent] =
      WithHeaders {
        WithRequestLogging {
          WithRequestAuditing {
            Action {
              request =>
                val encryptedUserId: Option[String] = request.session.get("userId")
                val token: Option[String] = request.session.get("token")
                if (encryptedUserId.isEmpty || token.isDefined) {
                  Logger.debug(s"No identity cookie found or wrong user type - redirecting to login. user : ${decrypt(encryptedUserId.getOrElse(""))} tokenDefined : ${token.isDefined}")
                  RedirectUtils.toSamlLogin
                } else {
                  act(decrypt(encryptedUserId.get), None, request, taxRegime, action)
                }
            }
          }
        }
      }

  }

  object AuthorisedForGovernmentGatewayAction {

    def apply(taxRegime: Option[TaxRegime] = None)(action: (User => (Request[AnyContent] => Result))): Action[AnyContent] =
      WithHeaders {
        WithRequestLogging {
          WithRequestAuditing {
            Action {
              request =>
                val encryptedUserId: Option[String] = request.session.get("userId")
                val token: Option[String] = request.session.get("token")
                if (encryptedUserId.isEmpty || token.isEmpty) {
                  // the redirect in this condition needs to be reviewed and updated. Important: It will be different from the AuthorisedForIdaAction redirect location
                  Logger.debug("No identity cookie found or no gateway token- redirecting to login. user : $userId tokenDefined : ${token.isDefined}")
                  Redirect(routes.HomeController.landing())
                } else {
                  act(decrypt(encryptedUserId.get), decrypt(token), request, taxRegime, action)
                }
            }
          }
        }
      }

  }

  object UnauthorisedAction {

    def apply[A <: TaxRegime](action: (Request[AnyContent] => Result)): Action[AnyContent] =
      WithHeaders {
        WithRequestLogging {
          WithRequestAuditing {
            Action {
              request =>
                action(request)
            }
          }
        }
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
    vat = regimes.vat match {
      case Some(x: URI) => Some(vatMicroService.root(x.toString))
      case _ => None
    }
  )
}
