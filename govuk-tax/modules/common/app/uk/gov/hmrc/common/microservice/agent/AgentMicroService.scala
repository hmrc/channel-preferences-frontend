package uk.gov.hmrc.common.microservice.agent

import uk.gov.hmrc.microservice.{ MicroService, MicroServiceConfig }
import play.api.libs.json.Json
import controllers.common.domain.Transform._

class AgentMicroService(override val serviceUrl: String = MicroServiceConfig.auditServiceUrl) extends MicroService {

  def create(newAgent: Agent): Option[Agent] = httpPost[Agent](s"/agent/", Json.parse(toRequestBody(newAgent)))
}
