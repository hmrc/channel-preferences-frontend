package uk.gov.hmrc.common.microservice.agent

import uk.gov.hmrc.microservice._
import play.api.libs.json.Json
import controllers.common.domain.Transform._
import uk.gov.hmrc.domain.Uar

class AgentMicroService(override val serviceUrl: String = MicroServiceConfig.agentServiceUrl) extends MicroService {

  def root(uri: String) = httpGet[AgentRoot](uri).getOrElse(throw new IllegalStateException(s"Expected Agent root not found at URI '$uri'"))

  def create(nino: String, newAgent: AgentRegistrationRequest): Uar = {
    val uar = httpPost[Uar](s"/agent/register/nino/$nino", Json.parse(toRequestBody(newAgent)))
    uar.getOrElse(throw new RuntimeException("Unexpected error creating a new agent"))
  }

  def searchClient(searchRequest: SearchRequest): Option[MatchingPerson] = httpPost[MatchingPerson](s"/agent/search", Json.parse(toRequestBody(searchRequest)))
}

trait AgentMicroServices {

  implicit lazy val agentMicroService = new AgentMicroService()
}
