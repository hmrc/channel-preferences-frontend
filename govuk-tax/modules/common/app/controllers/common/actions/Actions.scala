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
import scala.deprecated

trait Actions
  extends Results
  with CookieEncryption
  with AuditActionWrapper
  with SessionTimeoutWrapper
  with ServiceRoots
  with UserActionWrapper {

  def AuthorisedFor(account: TaxRegime,
                    redirectToOrigin: Boolean = false,
                    pageVisibility: PageVisibilityPredicate = DefaultPageVisibilityPredicate)
                   (body: User => (Request[AnyContent] => SimpleResult)) =
    authorised(account.authenticationType, Some(account), redirectToOrigin, pageVisibility, body)

  def AuthorisedBy(authenticationProvider: AuthenticationProvider,
                   redirectToOrigin: Boolean = false)
                  (body: User => (Request[AnyContent] => SimpleResult)) =
    authorised(authenticationProvider, None, redirectToOrigin, DefaultPageVisibilityPredicate, body)

  private def authorised(authenticationProvider: AuthenticationProvider,
                         account: Option[TaxRegime],
                         redirectToOrigin: Boolean,
                         pageVisibility: PageVisibilityPredicate,
                         body: User => (Request[AnyContent] => SimpleResult)) =
    WithHeaders {
      WithRequestLogging {
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

  @deprecated("Use AuthorisedFor or AuthorisedBy instead", "18/nov/2013")
  def ActionAuthorisedBy(authenticationType: AuthenticationProvider)
                        (taxRegime: Option[TaxRegime] = None, redirectToOrigin: Boolean = false)
                        (body: (User => (Request[AnyContent] => SimpleResult))): Action[AnyContent] =
    ActionAuthorisedByWithVisibility(authenticationType)(taxRegime, redirectToOrigin)(DefaultPageVisibilityPredicate)(body)

  @deprecated("Use AuthorisedFor or AuthorisedBy instead", "18/nov/2013")
  def ActionAuthorisedByWithVisibility(authenticationType: AuthenticationProvider)
                                      (taxRegime: Option[TaxRegime] = None, redirectToOrigin: Boolean = false)
                                      (pageVisibilityPredicate: PageVisibilityPredicate)
                                      (body: (User => (Request[AnyContent] => SimpleResult))): Action[AnyContent] =
    authorised(authenticationType, taxRegime, redirectToOrigin, pageVisibilityPredicate, body)

  def UnauthorisedAction(body: (Request[AnyContent] => SimpleResult)): Action[AnyContent] =
    WithHeaders {
      WithRequestLogging {
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
