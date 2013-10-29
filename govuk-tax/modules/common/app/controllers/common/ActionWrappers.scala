package controllers.common

import controllers.common.service._
import com.google.common.net.HttpHeaders
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.domain.RegimeRoots

trait HeaderNames {
  val requestId = "X-Request-ID"
  val authorisation = HttpHeaders.AUTHORIZATION
  val forwardedFor = "x-forwarded-for"
  val xSessionId = "X-Session-ID"
}

object HeaderNames extends HeaderNames

@deprecated("please use Actions", "24.10.13")
trait ActionWrappers
  extends MicroServices
  with Actions  {

  override def regimeRoots(authority: UserAuthority): RegimeRoots = {

    import uk.gov.hmrc.common.microservice.ct.domain.CtDomain.CtRoot
    import uk.gov.hmrc.common.microservice.epaye.domain.EpayeDomain.EpayeRoot
    import uk.gov.hmrc.common.microservice.vat.domain.VatDomain.VatRoot
    import uk.gov.hmrc.common.microservice.sa.domain.SaDomain.SaRoot


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
