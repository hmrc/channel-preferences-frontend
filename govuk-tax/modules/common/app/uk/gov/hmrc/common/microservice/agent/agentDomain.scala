package uk.gov.hmrc.common.microservice.agent

import uk.gov.hmrc.common.microservice.domain.{TaxRegime, Address}
import uk.gov.hmrc.domain.{Uar, SaUtr}
import uk.gov.hmrc.utils.DateConverter
import uk.gov.hmrc.common.microservice.auth.domain.Regimes
import controllers.common.routes


object AgentRegime extends TaxRegime {
  override def isAuthorised(regimes: Regimes) = {
    regimes.agent.isDefined
  }

  override def unauthorisedLandingPage: String = routes.LoginController.login().url
}

case class AgentRoot(uar: String, clients: Map[String, String], actions: Map[String, String])
