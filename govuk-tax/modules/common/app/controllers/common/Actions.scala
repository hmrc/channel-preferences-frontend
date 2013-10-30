package controllers.common

import play.api.mvc._
import controllers.common.actions.{UserActionWrapper, AuditActionWrapper}
import uk.gov.hmrc.common.microservice.domain._
import controllers.common.service.MicroServices
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import controllers.common.actions.{WithRequestLogging, WithHeaders}
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRoot

trait Actions
  extends Results
  with CookieEncryption
  with AuditActionWrapper
  with SessionTimeoutWrapper
  with ServiceRoots
  with UserActionWrapper {

  def ActionAuthorisedBy(authenticationType: AuthenticationType)
                        (taxRegime: Option[TaxRegime] = None, redirectToOrigin: Boolean = false)
                        (body: (User => (Request[AnyContent] => SimpleResult))): Action[AnyContent] = {
    WithHeaders {
      WithRequestLogging {
        WithSessionTimeoutValidation {
          WithUserAuthorisedBy(authenticationType)(taxRegime, redirectToOrigin) {
            user =>
              WithRequestAuditing(user) {
                user => Action(body(user))
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

  import MicroServices._

  /**
   * NOTE: THE DEFAULT IMPLEMENTATION WILL BE REMOVED SHORTLY
   */
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
