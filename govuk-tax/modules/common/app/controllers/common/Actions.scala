package controllers.common

import play.api.mvc._
import controllers.common.actions.{LoggingActionWrapper, AuditActionWrapper, HeaderActionWrapper}
import uk.gov.hmrc.common.microservice.domain._
import play.api.Logger
import views.html.login
import controllers.common.FrontEndRedirect._
import controllers.common.service.MicroServices
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import scala.Some
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots

trait Actions
  extends Results
  with CookieEncryption
  with HeaderActionWrapper
  with AuditActionWrapper
  with SessionTimeoutWrapper
  with LoggingActionWrapper
  with ServiceRoots {

  import uk.gov.hmrc.common.microservice.auth.AuthMicroService

  implicit val authMicroService : AuthMicroService

  def act(userId: String, token: Option[String],
          request: Request[AnyContent],
          taxRegime: Option[TaxRegime],
          action: (User) => (Request[AnyContent]) => SimpleResult): SimpleResult = {
    val userAuthority = authMicroService.authority(userId)
    Logger.debug(s"Received user authority: $userAuthority")

    userAuthority match {
      case (Some(ua)) => {
        taxRegime match {
          case Some(regime) if !regime.isAuthorised(ua.regimes) =>
            Logger.info("user not authorised for " + regime.getClass)
            Redirect(regime.unauthorisedLandingPage)
          case _ =>
            val user = User(
              userId = userId,
              userAuthority = ua,
              regimes = regimeRoots(ua),
              nameFromGovernmentGateway = decrypt(request.session.get("name")),
              decryptedToken = token)

            auditRequest(user, request)
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

    def apply(taxRegime: Option[TaxRegime] = None, redirectToOrigin: Boolean = false)(action: (User => (Request[AnyContent] => SimpleResult))): Action[AnyContent] =
      WithHeaders {
        WithRequestLogging {
          WithSessionTimeoutValidation {
            // FIXME - Code should be copied from ActionWrappers
            WithRequestAuditing(None) {
              Action {
                request =>
                  val encryptedUserId = request.session.get("userId")
                  val token = request.session.get("token")

                  if (encryptedUserId.isEmpty || token.isDefined) {
                    Logger.info(s"No identity cookie found or wrong user type - redirecting to login. user : ${decrypt(encryptedUserId.getOrElse(""))} tokenDefined : ${token.isDefined}")
                    val redirectUrl = if (redirectToOrigin) Some(request.uri) else None
                    toSamlLogin.withSession(buildSessionForRedirect(request.session, redirectUrl))
                  } else {
                    act(userId = decrypt(encryptedUserId.get), token = None, request = request, taxRegime = taxRegime, action = action)
                  }
              }
            }
          }
        }
      }
  }

  object AuthorisedForGovernmentGatewayAction {

    def apply(taxRegime: Option[TaxRegime] = None)(action: (User => (Request[AnyContent] => SimpleResult))): Action[AnyContent] =
      WithHeaders {
        WithRequestLogging {
          WithSessionTimeoutValidation {
            // FIXME - Code should be copied from ActionWrappers
            WithRequestAuditing(None) {
              Action {
                request =>
                  val encryptedUserId = request.session.get("userId")
                  val token = request.session.get("token")
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

  }

  object UnauthorisedAction {

    def apply[A <: TaxRegime](action: (Request[AnyContent] => SimpleResult)): Action[AnyContent] =
      WithHeaders {
        WithRequestLogging {
          // FIXME - Code should be copied from ActionWrappers
          WithRequestAuditing(None) {
            Action {
              request =>
                action(request)
            }
          }
        }
      }
  }

}


trait ServiceRoots {

  import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.CtRoot
  import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeRoot
  import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRoot
  import uk.gov.hmrc.common.microservice.sa.domain.SaDomain.SaRoot
  import MicroServices._

  def regimeRoots(authority: UserAuthority): RegimeRoots = {
    val regimes = authority.regimes
    RegimeRoots(
      paye = regimes.paye map {
        uri => payeMicroService.root(uri.toString)
      },
      sa = regimes.sa map {
        uri => SaRoot(authority.saUtr.get, saConnector.root(uri.toString))
      },
      vat = regimes.vat map {
        uri => VatRoot(authority.vrn.get, vatConnector.root(uri.toString))
      },
      epaye = regimes.epaye.map {
        uri => EpayeRoot(authority.empRef.get, epayeConnector.root(uri.toString))
      },
      ct = regimes.ct.map {
        uri => CtRoot(authority.ctUtr.get, ctConnector.root(uri.toString))
      },
      agent = regimes.agent.map {
        uri => agentMicroServiceRoot.root(uri.toString)
      }
    )
  }
}
