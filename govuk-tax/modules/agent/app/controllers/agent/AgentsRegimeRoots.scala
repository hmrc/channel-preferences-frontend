package controllers.agent

import scala.concurrent._
import controllers.common.RegimeRootBase

import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority

trait AgentsRegimeRoots extends RegimeRootBase {
  def regimeRoots(authority: UserAuthority)(implicit hc: HeaderCarrier): Future[RegimeRoots] = {
    val roots = (payeRoot(authority),saRoot(authority),vatRoot(authority),epayeRoot(authority),ctRoot(authority),agentRoot(authority))
    for {
      paye <- roots._1
      sa <- roots._2
      vat <- roots._3
      epaye <- roots._4
      ct <- roots._5
      agent <- roots._6
    } yield RegimeRoots(paye = paye, sa = sa, vat = vat, epaye = epaye, ct = ct, agent = agent)
  }
}
