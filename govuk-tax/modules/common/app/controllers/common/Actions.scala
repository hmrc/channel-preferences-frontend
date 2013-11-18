package controllers.common

import play.api.mvc._
import controllers.common.actions._
import uk.gov.hmrc.common.microservice.domain._
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRoot
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import controllers.common.actions.WithHeaders
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import play.api.mvc.SimpleResult

trait Actions
  extends Results
  with CookieEncryption
  with AuditActionWrapper
  with SessionTimeoutWrapper
  with ServiceRoots
  with UserActionWrapper {

  // MAT: TODO I think that this is the only authorised method signature that we actually need.
  //           I think also that we're going a bit far with the number of method parameter lists. We don't need them.
  //           and defaulting the parameters and using named parameters would give a far simpler signature.
  //           I'm calling the TaxRegime param "account" as the auth refactor will call these things accounts.
  //
  //           With this we'd be able to say:
  //
  //           AuthorisedFor(Paye) {
  //              //doStuff()
  //           ]
  //
  //           or
  //
  //           AuthorisedFor(account = Paye, pageVisibility = ...) {
  //               // doStuff
  //           }
  //
  //           etc
//  def AuthorisedFor(account: TaxRegime,
//                    redirectToOrigin: Boolean = false,
//                    pageVisibility: PageVisibilityPredicate = DefaultPageVisibilityPredicate)
//                   (body: Action[AnyContent]) = {
//    // TODO
//  }

  def ActionAuthorisedBy(authenticationType: AuthenticationType)
                        (taxRegime: Option[TaxRegime] = None, redirectToOrigin: Boolean = false)
                        (body: (User => (Request[AnyContent] => SimpleResult))): Action[AnyContent] = {
    ActionAuthorisedByWithVisibility(authenticationType)(taxRegime, redirectToOrigin)(DefaultPageVisibilityPredicate)(body)
  }

  def ActionAuthorisedByWithVisibility(authenticationType: AuthenticationType)
                                      (taxRegime: Option[TaxRegime] = None, redirectToOrigin: Boolean = false)
                                      (pageVisibilityPredicate: PageVisibilityPredicate)
                                      (body: (User => (Request[AnyContent] => SimpleResult))): Action[AnyContent] = {
    WithHeaders {
      WithRequestLogging {
        WithSessionTimeoutValidation {
          WithUserAuthorisedBy(authenticationType)(taxRegime, redirectToOrigin) {
            user =>
              WithPageVisibility(pageVisibilityPredicate, user) {
                user =>
                  WithRequestAuditing(user) {
                    user => Action(body(user))
                  }
              }
          }
        }
      }
    }
  }

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
