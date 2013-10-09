package uk.gov.hmrc.common.microservice.agent

import uk.gov.hmrc.microservice.{ MicroService, MicroServiceConfig }
import play.api.libs.json.Json
import controllers.common.domain.Transform._
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot

class AgentMicroService(override val serviceUrl: String = MicroServiceConfig.agentServiceUrl) extends MicroService {

  def root(uri: String) = httpGet[Agent](uri).getOrElse(throw new IllegalStateException(s"Expected Agent root not found at URI '$uri'"))

  def create(nino: String, newAgent: Agent): Option[Agent] = httpPost[Agent](s"/agent/register/nino/$nino", Json.parse(toRequestBody(newAgent)))

  def searchClient(searchRequest: SearchRequest): Option[MatchingPerson] = httpPost[MatchingPerson](s"/agent/search", Json.parse(toRequestBody(searchRequest)))
}

trait AgentMicroServices {

  implicit lazy val agentMicroService = new AgentMicroService()
}
