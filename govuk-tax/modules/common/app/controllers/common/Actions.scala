package controllers.common

import play.api.mvc._
import controllers.common.actions.AuditActionWrapper
import uk.gov.hmrc.common.microservice.domain._
import play.api.Logger
import views.html.login
import controllers.common.service.MicroServices
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import scala.Some
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import scala.concurrent.Future

trait Actions
  extends Results
  with CookieEncryption
  with AuditActionWrapper
  with SessionTimeoutWrapper
  with AuthorisationTypes
  with ServiceRoots {

  object ActionAuthorisedBy {

    import controllers.common.actions.{WithRequestLogging, WithHeaders}

    def apply(authenticationType: AuthorisationType)(taxRegime: Option[TaxRegime] = None, redirectToOrigin: Boolean = false)(action: (User => (Request[AnyContent] => SimpleResult))): Action[AnyContent] = {
      def handleAuthorised(request: Request[AnyContent]): PartialFunction[(Option[String], Option[String]), Either[User, SimpleResult]] = {
        case (Some(encryptedUserId), tokenOption) =>
          val userId = decrypt(encryptedUserId)
          val token = tokenOption.map(decrypt)
          val userAuthority = authMicroService.authority(userId)
          Logger.debug(s"Received user authority: $userAuthority")

          userAuthority match {
            case (Some(ua)) => {
              taxRegime match {
                case Some(regime) if !regime.isAuthorised(ua.regimes) =>
                  Logger.info("user not authorised for " + regime.getClass)
                  Right(Redirect(regime.unauthorisedLandingPage))
                case _ =>
                  Left(User(
                    userId = userId,
                    userAuthority = ua,
                    regimes = regimeRoots(ua),
                    nameFromGovernmentGateway = decrypt(request.session.get("name")),
                    decryptedToken = token))
              }
            }
            case _ => {
              Logger.warn(s"No authority found for user id '$userId' from '${request.remoteAddress}'")
              Right(Unauthorized(login()).withNewSession)
            }
          }
      }
      WithHeaders {
        WithRequestLogging {
          WithSessionTimeoutValidation {
            Action.async { request =>
              val handle = authenticationType.handleNotAuthorised(request, redirectToOrigin) orElse handleAuthorised(request)
              val userOrFailureResult: Either[User, SimpleResult] = handle((request.session.get("userId"), request.session.get("token")))
              userOrFailureResult match {
                case Left(user) => WithRequestAuditing(Some(user))(Action { action(user)(request)})(request)
                case Right(result) => Future.successful(result)
              }
            }
          }
        }
      }
    }
  }

  object UnauthorisedAction {

    import controllers.common.actions.{WithRequestLogging, WithHeaders}

    def apply[A <: TaxRegime](action: (Request[AnyContent] => SimpleResult)): Action[AnyContent] =
      WithHeaders {
        WithRequestLogging {
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
