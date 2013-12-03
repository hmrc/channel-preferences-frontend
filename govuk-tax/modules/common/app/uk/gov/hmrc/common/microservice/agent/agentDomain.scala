package uk.gov.hmrc.common.microservice.agent

import uk.gov.hmrc.common.microservice.domain.TaxRegime
import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import controllers.common.{Ida, routes}

object AgentRegime extends TaxRegime {
  def isAuthorised(accounts: Accounts) = accounts.agent.isDefined

  def unauthorisedLandingPage = routes.LoginController.login().url

  def authenticationType = Ida
}

case class AgentRoot(uar: String, clients: Map[String, String], actions: Map[String, String])
