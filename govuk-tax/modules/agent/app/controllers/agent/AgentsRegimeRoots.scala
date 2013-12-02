package controllers.agent

import scala.concurrent._
import controllers.common.RegimeRootBase

import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.auth.domain.Authority

trait AgentsRegimeRoots extends RegimeRootBase {
  def regimeRoots(authority: Authority)(implicit hc: HeaderCarrier): Future[RegimeRoots] = {
    val roots = (payeRoot(authority),agentRoot(authority))
    for {
      paye <- roots._1
      agent <- roots._2
    } yield RegimeRoots(agent = agent, paye = paye)
  }
}
