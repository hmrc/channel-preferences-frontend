package controllers.common.actions

import play.api.mvc._
import uk.gov.hmrc.common.microservice.domain._
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRoot
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import controllers.common.{AuthenticationProvider, SessionTimeoutWrapper, CookieEncryption}

trait Actions
  extends Results
  with MdcHeaders
  with RequestLogging
  with AuditActionWrapper
  with SessionTimeoutWrapper
  with ServiceRoots
  with UserActionWrapper {

  private type PlayRequest = (Request[AnyContent] => SimpleResult)
  private type PlayUserRequest = User => PlayRequest

  def AuthorisedFor(account: TaxRegime,
                    redirectToOrigin: Boolean = false,
                    pageVisibility: PageVisibilityPredicate = DefaultPageVisibilityPredicate)
                   (body: PlayUserRequest) =
    authorised(account.authenticationType, Some(account), redirectToOrigin, pageVisibility, body)

  def AuthorisedBy(authenticationProvider: AuthenticationProvider,
                   redirectToOrigin: Boolean = false)
                  (body: PlayUserRequest) =
    authorised(authenticationProvider, None, redirectToOrigin, DefaultPageVisibilityPredicate, body)

  private def authorised(authenticationProvider: AuthenticationProvider,
                         account: Option[TaxRegime],
                         redirectToOrigin: Boolean,
                         pageVisibility: PageVisibilityPredicate,
                         body: PlayUserRequest) =
    storeHeaders {
      logRequest {
        WithSessionTimeoutValidation {
          WithUserAuthorisedBy(authenticationProvider, account, redirectToOrigin) {
            user =>
              WithPageVisibility(pageVisibility, user) {
                user =>
                  WithRequestAuditing(user) {
                    user => Action(body(user))
                  }
              }
          }
        }
      }
    }

  def UnauthorisedAction(body: PlayRequest): Action[AnyContent] =
    storeHeaders {
      logRequest {
        WithRequestAuditing {
          Action(body)
        }
      }
    }
}


trait ServiceRoots {

  import Connectors._

  /**
   * NOTE: THE DEFAULT IMPLEMENTATION WILL BE REMOVED SHORTLY
   */
  def regimeRoots(authority: UserAuthority): RegimeRoots = {
    val regimes = authority.regimes
    RegimeRoots(
      paye = regimes.paye map {
        uri => payeConnector.root(uri.toString)
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
        uri => agentConnectorRoot.root(uri.toString)
      }
    )
  }
}
