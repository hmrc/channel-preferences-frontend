package uk.gov.hmrc.common.microservice.agent

import uk.gov.hmrc.microservice._
import play.api.libs.json.Json
import controllers.common.domain.Transform._
import uk.gov.hmrc.domain.Uar
import play.api.libs.ws.Response

class AgentConnectorRoot(override val serviceUrl: String = MicroServiceConfig.agentServiceUrl) extends Connector {

  def root(uri: String) = httpGet[AgentRoot](uri).getOrElse(throw new IllegalStateException(s"Expected Agent root not found at URI '$uri'"))

}
