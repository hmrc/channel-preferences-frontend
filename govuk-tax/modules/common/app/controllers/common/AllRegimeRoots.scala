package controllers.common


import scala.concurrent._
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority


trait AllRegimeRoots extends RegimeRootBase {
  def regimeRoots(authority: UserAuthority)(implicit hc: HeaderCarrier): Future[RegimeRoots] = {
    for {
      paye <- payeRoot(authority)
      sa <- saRoot(authority)
      vat <- vatRoot(authority)
      epaye <- epayeRoot(authority)
      ct <- ctRoot(authority)
      agent <- agentRoot(authority)
    } yield RegimeRoots(paye = paye, sa = sa, vat = vat, epaye = epaye, ct = ct, agent = agent)
  }
}
