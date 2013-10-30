package uk.gov.hmrc.common.microservice.agent

import uk.gov.hmrc.common.microservice.domain.TaxRegime
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import controllers.common.{GovernmentGateway, routes}

object AgentRegime extends TaxRegime {
  def isAuthorised(regimes: Regimes) = regimes.agent.isDefined

  def unauthorisedLandingPage = routes.LoginController.login().url

  def authenticationType = GovernmentGateway
}

case class AgentRoot(uar: String, clients: Map[String, String], actions: Map[String, String])
