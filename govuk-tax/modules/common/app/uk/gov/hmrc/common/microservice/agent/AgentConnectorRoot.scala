package uk.gov.hmrc.common.microservice.agent

import uk.gov.hmrc.microservice._
import controllers.common.actions.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

class AgentConnectorRoot(override val serviceUrl: String = MicroServiceConfig.agentServiceUrl) extends Connector {

  def root(uri: String)(implicit hc: HeaderCarrier) =
    httpGetF[AgentRoot](uri).map(_.getOrElse(throw new IllegalStateException(s"Expected Agent root not found at URI '$uri'")))

}
