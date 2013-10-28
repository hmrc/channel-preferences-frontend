package controllers.common

import play.api.mvc._
import controllers.common.actions.{UserActionWrapper, AuditActionWrapper}
import uk.gov.hmrc.common.microservice.domain._
import controllers.common.service.MicroServices
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots

trait Actions
  extends Results
  with CookieEncryption
  with AuditActionWrapper
  with SessionTimeoutWrapper
  with AuthorisationTypes
  with ServiceRoots
  with UserActionWrapper {

  object ActionAuthorisedBy {

    import controllers.common.actions.{WithRequestLogging, WithHeaders}

    def apply(authenticationType: AuthorisationType)
             (taxRegime: Option[TaxRegime] = None, redirectToOrigin: Boolean = false)
             (body: (User => (Request[AnyContent] => SimpleResult))): Action[AnyContent] = {
      WithHeaders {
        WithRequestLogging {
          WithSessionTimeoutValidation {
            WithUserAuthorisedBy(authenticationType)(taxRegime, redirectToOrigin) { user =>
              WithRequestAuditing(user) {
                user: User => Action(body(user))
              }
            }
          }
        }
      }
    }
  }

  object UnauthorisedAction {

    import controllers.common.actions.{WithRequestLogging, WithHeaders}

    def apply[A <: TaxRegime](body: (Request[AnyContent] => SimpleResult)): Action[AnyContent] =
      WithHeaders {
        WithRequestLogging {
          WithRequestAuditing {
            Action(body)
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
